package akka.mobile.client

import scala.collection._
import java.net.InetSocketAddress
import akka.actor._
import akka.config.Supervision._
import com.eaio.uuid.UUID
import akka.mobile.communication.CommunicationMessages._
import akka.mobile.communication.NetworkFailures._
import akka.mobile.communication._
import akka.util.Duration
import akka.dispatch.{FutureTimeoutException, CompletableFuture, Dispatchers}
import java.io.IOException
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol


/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */


object RemoteMessaging {
  val DEFAULT_TCP_SOCKET_FACTOR = (address: InetSocketAddress)
  => new TCPSocket(address, MobileConfiguration.defaultConfig.CONNECT_TIMEOUT.toMillis.toInt)

  def apply(clientID: ClientId, errorHandler: ActorRef)
  = new RemoteMessaging(DEFAULT_TCP_SOCKET_FACTOR, MobileConfiguration.defaultConfig, errorHandler, clientID)


  def apply(socketFactory: InetSocketAddress => SocketRepresentation, clientID: ClientId, errorHandler: ActorRef)
  = new RemoteMessaging(socketFactory, MobileConfiguration.defaultConfig, errorHandler, clientID)
}

class RemoteMessaging(socketFactory: InetSocketAddress => SocketRepresentation,
                      config: MobileConfiguration,
                      faultHandling: ActorRef,
                      clientId: ClientId) {

  val registry = new Registry()
  val futures: FutureResultHandling = new FutureResultHandling
  private val messangers = scala.collection.mutable.Map[InetSocketAddress, ActorRef]()
  val msgSink: MessageSink = new MessageSink() {

    def sendResponse(nodeId: Either[ClientId, InetSocketAddress],
                     responseFor: UUID, result: Right[Throwable, Any]) {

      nodeId match {
        case Right(remoteAddress) => {
          val msg = serializer.toWireProtocol(
            serializer.response(responseFor, result))
          val remoteChannel = channelFor(remoteAddress)
          remoteChannel ! SendMessage(msg, None)
        }

        case Left(_) => throw new IllegalArgumentException("Cannot send to a client from a client")
      }
    }

    def send(clientId: Either[ClientId, InetSocketAddress],
             serviceId: Either[String, UUID], message: Any,
             senderOption: Option[ActorRef],
             replyUUID: Option[UUID]) {
      clientId match {
        case Right(remoteAddress) => {
          val theMessage = serializer.toWireProtocol(
            serializer.messageToActor(serviceId, senderOption, message, replyUUID))
          val remoteChannel = channelFor(remoteAddress)
          remoteChannel ! SendMessage(theMessage, senderOption)

        }
        case Left(_) => throw new IllegalArgumentException("Cannot send to a client from a client")
      }
    }

    def registerFuture(uuid: UUID, future: CompletableFuture[Any]) {
      futures.put(uuid, future)
    }
  }
  private val serializer = new ClientSideSerialisation(msgSink, clientId)
  val wireMsgDispatcher = new WireMessageDispatcher(futures, registry, msgSink, serializer);

  /**
   * Returns the actor which is responsible for remote messaging with the given server
   * The actor communicates with the messages types of {@link akka.mobile.remote.RemoteMessage}
   */
  private def newCommunicationActor(address: InetSocketAddress): ActorRef = {
    Actor.actorOf(new RemoteMessagingSupervision(address, wireMsgDispatcher)).start()
  }


  def closeConnections() {
    messangers.synchronized {
      messangers.foreach(i => {
        i._2 ! CloseConnection
      })
    }
  }

  def channelFor(address: InetSocketAddress): ActorRef = {
    messangers.synchronized {
      messangers.getOrElseUpdate(address, newCommunicationActor(address))
    }
  }


  class RemoteMessagingSupervision(address: InetSocketAddress,
                                   wireMsgDispatcher: WireMessageDispatcher) extends Actor {
    self.faultHandler = AllForOneStrategy(List(classOf[IOException],
      classOf[FutureTimeoutException]), 5, 5000)

    var sendActor: ActorRef = null;
    var supervisor: Supervisor = null;
    var messages = mutable.Set[SendMessage]()

    override def preStart() {
      faultHandling ! StartedConnecting(self)
      initializeIOSubsystem()
    }

    protected def receive = {
      case msg: MaximumNumberOfRestartsWithinTimeRangeReached => {
        val lastException = msg.getLastExceptionCausingRestart
        faultHandling ! ConnectionError(lastException, messages.toList, self)
        become({
          case msg: MaximumNumberOfRestartsWithinTimeRangeReached => {
            // we are already in the right state
          }
          case PleaseTryToReconnect => {
            reconnect()
          }
          case msg: SendMessage => {
            faultHandling ! CannotSendDueNoConnection(lastException, msg, self)
          }
          case msg: SendingSucceeded => {
            messages.remove(msg.orignalMessage)
          }
          case CloseConnection => {
            unbecome()
            closeConnection()
          }
          case msg: CommunicationMessage => {
            // Ignored
          }
        });
      }
      case msg: SendMessage => {
        messages.add(msg)
        sendActor.!(msg)
      }
      case msg: SendingSucceeded => {
        messages.remove(msg.orignalMessage)
      }
      case CloseConnection => {
        closeConnection()
      }
      case msg: CommunicationMessage => {
        // Ignored
      }
      case msg: FailureHandlingMessages => {
        // Ignored
      }
    }

    private def reconnect() {
      faultHandling ! StartedConnecting(self)
      unbecome()
      initializeIOSubsystem()
    }

    private def closeConnection() {
      become({
        case msg: MaximumNumberOfRestartsWithinTimeRangeReached => {
          // we don't care at this point in time
          println("wtf")
        }
        case PleaseTryToReconnect => {
          reconnect()
        }
        case msg: SendMessage => {
          faultHandling ! CannotSendDueClosedConnection(new IOException("Connection has been closed"), msg, self)
        }
        case msg: SendingSucceeded => {
          messages.remove(msg.orignalMessage)
        }
        case msg: CommunicationMessage => {
          // Ignored
        }
      })
      supervisor.shutdown()
    }

    def initializeIOSubsystem() {
      val socketInitialisation = Actor.actorOf(ResourceInitializeActor[RemoteMessageChannel](
        () => new RemoteMessageChannel(socketFactory(address)), (c: RemoteMessageChannel) => c.close()))

      sendActor = Actor.actorOf(new RemoteMessageSendingActor(socketInitialisation, config.CONNECT_TIMEOUT))

      val receiveActor = Actor.actorOf(new ReceiveChannelMonitoring(socketInitialisation, wireMsgDispatcher,
        address, config.CONNECT_TIMEOUT))

      supervisor = Supervisor(SupervisorConfig(
        AllForOneStrategy(List(classOf[IOException], classOf[FutureTimeoutException]),
          config.RETRIES_ON_CONNECTION_FAILURES,
          config.RETRIES_TIME_FRAME.toMillis.asInstanceOf[Int]),
        Supervise(socketInitialisation, Permanent) :: Supervise(sendActor, Permanent) :: Supervise(receiveActor, Permanent) :: Nil,
        (a, error) => {
          self ! error
        }))
    }
  }

}


/**
 *
 * Since remote messaging over very unreliable channels is difficult to manage,
 * we want to encapsulate the state-changes into an actor.
 */
class RemoteMessageSendingActor(channelProvider: ActorRef, timeout: Duration) extends Actor {
  protected def receive = {
    case fullMsg: SendMessage => {
      val channel =
        try {
          (channelProvider ? ResourceInitializeActor.GetResource)
            .await(timeout).resultOrException.get.asInstanceOf[RemoteMessageChannel]
        } catch {
          case e: IOException => {
            self.channel.tryTell(SendingFailed(e, fullMsg))
            throw e
          }
        }
      val sendThisMessageActor = Actor.actorOf(new RemoteMessageSendingActor(channel))
      self.link(sendThisMessageActor)
      sendThisMessageActor.start()
      sendThisMessageActor.!(fullMsg)(self.channel)

    }
  }

  class RemoteMessageSendingActor(channel: RemoteMessageChannel) extends Actor {

    protected def receive = {
      case CommunicationMessages.SendMessage(msg, sender) => {
        try {
          channel.send(msg)
          self.channel.tryTell(SendingSucceeded(SendMessage(msg, sender)))
        } catch {
          case e: IOException => {
            channel.close()
            self.channel.tryTell(SendingFailed(e, SendMessage(msg, sender)))
            throw e
          }
        }
        self.stop()
      }
    }

    override def preRestart(reason: Throwable, message: Option[Any]) {
      message.foreach(m => {
        self.!(m)(self.channel)
      })

    }
  }

}

class ReceiveChannelMonitoring(channelProvider: ActorRef,
                               dispatcher: WireMessageDispatcher,
                               ctxInfo: InetSocketAddress,
                               timeout: Duration) extends Actor {


  override def preStart() {
    self ! "Receive"
  }


  protected def receive = {
    case "Receive" => {
      try {
        val channel =
          (channelProvider ? ResourceInitializeActor.GetResource)
            .await(timeout).resultOrException.get.asInstanceOf[RemoteMessageChannel]

        val receiverThread = new ReceiveThread(channel, self)
        receiverThread.start()
      } catch {
        case e: IOException => {
          throw e
        }
      }
    }
    case ErrorOccured(ex) => {
      throw ex
    }
  }

  case class ErrorOccured(obj: Throwable)

  /**
   * We cannot let the blocking read operation run in a actor.
   * Because it's blocking it can block the stopping-process on a crash. That then
   * prevents closing the socket on another actor. Currently this is the strategy to close
   * the connection. The resource managine actor just closes the socket and just let the other actors fail.
   */
  class ReceiveThread(channel: RemoteMessageChannel,
                      messageHandler: ActorRef) extends Thread("Network Message Receiver") {
    override def run() {
      try {
        while(!Thread.currentThread().isInterrupted) {
          val msg = channel.receive()
          if (msg != null) {
            if (msg.hasMessage) {
              dispatcher.dispatchMessage(msg.getMessage, Right(ctxInfo))
            } else if (msg != msg) {
              messageHandler ! ErrorOccured(new Error("Not yet implemented"))
            }
          }
        }
      } catch {
        case e: IOException => {
          channel.close()
          messageHandler ! ErrorOccured(e)
        }
        case e: Throwable => {
          messageHandler ! ErrorOccured(e)
        }
      }

    }
  }

}


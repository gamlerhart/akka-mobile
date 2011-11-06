package akka.mobile.remote

import java.net.InetSocketAddress
import java.io.IOException
import akka.actor._
import akka.config.Supervision.{AllForOneStrategy, SupervisorConfig}
import akka.util.duration._
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import com.eaio.uuid.UUID
import akka.dispatch.{CompletableFuture, Dispatchers}


/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class RemoteMessaging(socketFactory: InetSocketAddress => SocketRepresentation,
                      config: MobileConfiguration,
                      clientId: ClientId) {
  val registry: Registry = new Registry
  val futures: FutureResultHandling = new FutureResultHandling
  private val messangers = scala.collection.mutable.Map[InetSocketAddress, ActorRef]()
  private val supervisor = Supervisor(SupervisorConfig(
    AllForOneStrategy(List(classOf[Exception]),
      config.RETRIES_ON_CONNECTION_FAILURES,
      config.RETRIES_TIME_FRAME.toMillis.asInstanceOf[Int]), Nil
  ))
  val msgSink: MessageSink = new MessageSink() {

    def sendResponse(clientId: Either[ClientId, InetSocketAddress],
                     responseFor: UUID, result: Right[Throwable, Any]): Unit = {

      clientId match {
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
             serviceId: String, message: Any,
             senderOption: Option[ActorRef],
             replyUUID: Option[UUID]) {
      clientId match {
        case Right(remoteAddress) => {
          registry.registerActor("uuid:" + senderOption.get.uuid.toString, senderOption.get)
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

  /**
   * Returns the actor which is responsible for remote messaging with the given server
   * The actor communicates with the messages types of {@link akka.mobile.remote.RemoteMessage}
   */
  private def newCommunicationActor(address: InetSocketAddress): ActorRef = {
    val socketInitialisation = Actor.actorOf(ResourceInitializeActor(
      () => new RemoteMessageChannel(socketFactory(address))))
    val sendActor = Actor.actorOf(new RemoteMessageSendingActor(socketInitialisation))
    val receiveActor = Actor.actorOf(new ReceiveChannelMonitoring(socketInitialisation,
      new WireMessageDispatcher(registry, futures, msgSink, serializer), address))
    supervisor.link(socketInitialisation)
    supervisor.link(sendActor)
    supervisor.link(receiveActor)
    socketInitialisation.start()
    receiveActor.start()
    sendActor.start()
  }

  def channelFor(address: InetSocketAddress): ActorRef = {
    messangers.synchronized {
      messangers.getOrElseUpdate(address, newCommunicationActor(address))
    }
  }
}


object RemoteMessaging {
  val DEFAULT_TCP_SOCKET_FACTOR = (address: InetSocketAddress) => new TCPSocket(address, MobileConfiguration.defaultConfig)

  def apply(clientID: ClientId) = new RemoteMessaging(DEFAULT_TCP_SOCKET_FACTOR, MobileConfiguration.defaultConfig, clientID)


  def apply(socketFactory: InetSocketAddress => SocketRepresentation, clientID: ClientId)
  = new RemoteMessaging(socketFactory, MobileConfiguration.defaultConfig, clientID)
}


/**
 *
 * Since remote messaging over very unreliable channels is difficult to manage,
 * we want to encapsulate the state-changes into an actor.
 */
class RemoteMessageSendingActor(channelProvider: ActorRef) extends Actor {
  protected def receive = {
    case SendMessage(msg, sender) => {
      val channel =
        try {
          (channelProvider ? ResourceInitializeActor.GetResource)
            .await(5.seconds).resultOrException.get.asInstanceOf[RemoteMessageChannel]
        } catch {
          case e: IOException => {
            self.channel.tryTell(SendingFailed(e, SendMessage(msg, sender)))
            throw e
          }
        }
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
    }
  }
}

class ReceiveChannelMonitoring(channelProvider: ActorRef,
                               dispatcher: WireMessageDispatcher,
                               ctxInfo: InetSocketAddress) extends Actor {
  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  var channel: Option[RemoteMessageChannel] = None

  override def preStart() {
    channel = None
    self ! "Receive"
  }

  protected def receive = {
    case "Receive" => {
      try {
        if (channel.isEmpty) {
          channel =
            Some(
              (channelProvider ? ResourceInitializeActor.GetResource)
                .await(5.seconds).resultOrException.get.asInstanceOf[RemoteMessageChannel])
        }
        val msg = channel.get.receive()
        if (msg != null) {
          if (msg.hasMessage) {
            dispatcher.dispatchMessage(msg.getMessage, Right(ctxInfo))
          } else if (msg != msg) {
            throw new Error("Not yet implemented")
          }
          self ! "Receive"
        }

      } catch {
        case e: IOException => {
          channel.foreach(_.close())
          throw e
        }
      }
    }
  }
}

trait RemoteMessage

case class SendMessage(msg: AkkaMobileProtocol, sender: Option[ActorRef]) extends RemoteMessage

case class IoOperationFailed(exception: Exception) extends RemoteMessage

case class SendingFailed(override val exception: Exception, orignalMessage: SendMessage) extends IoOperationFailed(exception)

case class SendingSucceeded(orignalMessage: SendMessage) extends RemoteMessage
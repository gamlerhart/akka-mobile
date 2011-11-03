package akka.mobile.remote

import java.net.InetSocketAddress
import java.io.IOException
import akka.dispatch.Dispatchers
import akka.actor._
import akka.config.Supervision.{AllForOneStrategy, SupervisorConfig}
import akka.util.duration._
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol


/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class RemoteMessaging(socketFactory: InetSocketAddress => SocketRepresentation, msgSink: MessageSink) {
  val registry: Registry = new Registry
  private val messangers = scala.collection.mutable.Map[InetSocketAddress, ActorRef]()
  private val supervisor = Supervisor(SupervisorConfig(
    AllForOneStrategy(List(classOf[Exception]), 5, 10000), Nil
  ))

  /**
   * Returns the actor which is responsible for remote messaging with the given server
   * The actor communicates with the messages types of {@link akka.mobile.remote.RemoteMessage}
   */
  private def newCommunicationActor(address: InetSocketAddress): ActorRef = {
    val socketInitialisation = Actor.actorOf(ResourceInitializeActor(
      () => new RemoteMessageChannel(socketFactory(address))))
    val sendActor = Actor.actorOf(new RemoteMessageSendingActor(socketInitialisation))
    val receiveActor = Actor.actorOf(new ReceiveChannelMonitoring(socketInitialisation,
      new WireMessageDispatcher(registry, new ClientSideSerialisation(msgSink))))
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
  val DEFAULT_TCP_SOCKET_FACTOR = (address: InetSocketAddress) => new TCPSocket(address)

  def apply(msgSink: MessageSink) = new RemoteMessaging(DEFAULT_TCP_SOCKET_FACTOR, msgSink)

  def apply(msgSink: () => MessageSink) = new RemoteMessaging(DEFAULT_TCP_SOCKET_FACTOR, msgSink())

  def apply(socketFactory: InetSocketAddress => SocketRepresentation, msgSink: MessageSink)
  = new RemoteMessaging(socketFactory, msgSink)
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

class ReceiveChannelMonitoring(channelProvider: ActorRef, dispatcher: WireMessageDispatcher) extends Actor {
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
            dispatcher.dispatchToActor(msg.getMessage)
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
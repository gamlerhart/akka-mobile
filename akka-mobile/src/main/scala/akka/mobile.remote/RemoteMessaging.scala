package akka.mobile.remote

import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import java.net.InetSocketAddress
import java.io.IOException
import akka.actor.{ActorRef, Actor}

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class RemoteMessaging(socketFactory: InetSocketAddress => SocketRepresentation) {
  private val messangers = scala.collection.mutable.Map[InetSocketAddress, ActorRef]()

  /**
   * Returns the actor which is responsible for remote messaging with the given server
   * The actor communicates with the messages types of {@link akka.mobile.remote.RemoteMessage}
   */
  private def newCommunicationActor(address: InetSocketAddress): ActorRef = {
    val actor = Actor.actorOf(new RemoteMessagingActor(() => new RemoteMessageChannel(socketFactory(address))))
    actor.start()
  }

  def channelFor(address: InetSocketAddress): ActorRef = {
    messangers.synchronized {
      messangers.getOrElseUpdate(address, newCommunicationActor(address))
    }
  }
}


object RemoteMessaging {
  val DEFAULT_TCP_SOCKET_FACTOR = (address: InetSocketAddress) => new TCPSocket(address)

  def apply() = new RemoteMessaging(DEFAULT_TCP_SOCKET_FACTOR)

  def apply(socketFactory: InetSocketAddress => SocketRepresentation) = new RemoteMessaging(socketFactory)
}


/**
 *
 * Since remote messaging over very unreliable channels is difficult to manage,
 * we want to encapsulate the state-changes into an actor.
 */
class RemoteMessagingActor(channelFactory: () => RemoteMessageChannel) extends Actor {
  private var channelOption: Option[RemoteMessageChannel] = None

  override def preStart() {
    channelOption = None
  }

  protected def receive = {
    case SendMessage(msg, sender) => {
      try {
        channel.send(msg)
        self.channel ! SendingSucceeded(SendMessage(msg, sender))
      } catch {
        case x: IOException => {
          channelOption.foreach(_.close())
          self.channel ! SendingFailed(x, SendMessage(msg, sender))
          throw x
        }
      }
    }
  }

  def channel: RemoteMessageChannel = {
    if (channelOption.isEmpty) {
      channelOption = Some(channelFactory())
    }
    channelOption.get
  }
}

trait RemoteMessage

case class SendMessage(msg: AkkaMobileProtocol, sender: Option[ActorRef]) extends RemoteMessage

case class SendingFailed(exception: Exception, orignalMessage: SendMessage) extends RemoteMessage

case class SendingSucceeded(orignalMessage: SendMessage) extends RemoteMessage
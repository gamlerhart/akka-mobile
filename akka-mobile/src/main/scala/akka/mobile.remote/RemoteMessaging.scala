package akka.mobile.remote

import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.actor.{ActorRef, Actor}
import java.net.InetSocketAddress


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
  private val channel = channelFactory()

  protected def receive = {
    case SendMessage(msg, sender) => {
      channel.send(msg)
    }
  }
}

trait RemoteMessage

case class SendMessage(msg: AkkaMobileProtocol, sender: Option[ActorRef]) extends RemoteMessage
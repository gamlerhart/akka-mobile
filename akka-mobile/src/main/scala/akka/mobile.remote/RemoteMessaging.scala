package akka.mobile.remote

import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import java.net.InetSocketAddress
import java.io.IOException
import akka.dispatch.Dispatchers
import akka.actor._
import akka.config.Supervision.{AllForOneStrategy, SupervisorConfig}

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class RemoteMessaging(socketFactory: InetSocketAddress => SocketRepresentation) {
  private val messangers = scala.collection.mutable.Map[InetSocketAddress, ActorRef]()
  private val supervisor = Supervisor(SupervisorConfig(
    AllForOneStrategy(List(classOf[Exception]), 5, 10000), Nil
  ))

  /**
   * Returns the actor which is responsible for remote messaging with the given server
   * The actor communicates with the messages types of {@link akka.mobile.remote.RemoteMessage}
   */
  private def newCommunicationActor(address: InetSocketAddress): ActorRef = {
    val socketInitialisation = Actor.actorOf(new ResourceInitializeActor(() => new RemoteMessageChannel(socketFactory(address))))
    val sendActor = Actor.actorOf(new RemoteMessageSendingActor(socketInitialisation))
    supervisor.link(socketInitialisation)
    supervisor.link(sendActor)
    socketInitialisation.start()
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

  def apply() = new RemoteMessaging(DEFAULT_TCP_SOCKET_FACTOR)

  def apply(socketFactory: InetSocketAddress => SocketRepresentation) = new RemoteMessaging(socketFactory)
}


/**
 *
 * Since remote messaging over very unreliable channels is difficult to manage,
 * we want to encapsulate the state-changes into an actor.
 */
class RemoteMessageSendingActor(channelFactory: ActorRef) extends Actor {

  import akka.util.duration._

  protected def receive = {
    case SendMessage(msg, sender) => {
      val channel =
        try {
          (channelFactory ? ResourceInitializeActor.GetResource)
            .await(5.seconds).resultOrException.get.asInstanceOf[RemoteMessageChannel]
        } catch {
          case e: IOException => {
            self.channel.tryTell(SendingFailed(e, SendMessage(msg, sender)))(null)
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

class ReceiveChannelMonitoring(channel: RemoteMessageChannel) extends Actor {
  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)


  protected def receive = {
    case "Start" => {
      try {
        val msg = channel.receive()

      }

    }

  }
}

trait RemoteMessage

case class SendMessage(msg: AkkaMobileProtocol, sender: Option[ActorRef]) extends RemoteMessage

case class SendingFailed(exception: Exception, orignalMessage: SendMessage) extends RemoteMessage

case class SendingSucceeded(orignalMessage: SendMessage) extends RemoteMessage
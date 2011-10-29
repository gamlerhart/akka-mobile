package akka.mobile.remote

import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import java.net.InetSocketAddress
import java.io.IOException
import akka.dispatch.Dispatchers
import akka.actor._
import akka.config.Supervision.{AllForOneStrategy, SupervisorConfig}
import akka.util.duration._


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
    val socketInitialisation = Actor.actorOf(ResourceInitializeActor(() => new RemoteMessageChannel(socketFactory(address))))
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
          channel = Some(
            (channelProvider ? ResourceInitializeActor.GetResource)
              .await(5.seconds).resultOrException.get.asInstanceOf[RemoteMessageChannel])
        }
        val msg = channel.get.receive()
        if (msg.hasMessage) {
          dispatcher.dispatchToActor(msg.getMessage)
        } else {
          throw new Error("Not yet implemented")
        }
        self ! "Receive"
      } catch {
        case e: IOException => {
          self.channel.tryTell(ReceivingFailed(e))
          throw e
        }
      }
    }

  }
}

trait RemoteMessage

case class SendMessage(msg: AkkaMobileProtocol, sender: Option[ActorRef]) extends RemoteMessage

case class IoOperationFailed(exception: Exception) extends RemoteMessage

case class ReceivingFailed(override val exception: Exception) extends IoOperationFailed(exception)

case class SendingFailed(override val exception: Exception, orignalMessage: SendMessage) extends IoOperationFailed(exception)

case class SendingSucceeded(orignalMessage: SendMessage) extends RemoteMessage
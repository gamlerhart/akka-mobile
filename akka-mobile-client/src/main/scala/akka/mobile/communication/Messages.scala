package akka.mobile.communication

import akka.actor.ActorRef
import java.net.InetSocketAddress
import akka.mobile.protocol.MobileProtocol.{AkkaMobileProtocol, AddressProtocol, RemoteActorRefProtocol}

/**
 * @author roman.stoffel@gamlor.info
 * @since 11.11.11
 */
/**
 * This is here to easily ignore all messages which you're not interested in.
 */
trait AkkaMobileControlMessage

trait CommunicationMessage extends AkkaMobileControlMessage

case object CommunicationMessages {

  case class SendMessage(msg: AkkaMobileProtocol, sender: Option[ActorRef]) extends CommunicationMessage


  case class IoOperationFailed(exception: Exception) extends CommunicationMessage

  case class SendingFailed(override val exception: Exception, originalMessage: SendMessage) extends IoOperationFailed(exception)

  case class SendingSucceeded(orignalMessage: SendMessage) extends CommunicationMessage

}

trait FailureHandlingMessages extends AkkaMobileControlMessage

case object NetworkFailures {

  /**
   * This failure message is sent when the connection fails and reconnection attempts failed.
   *
   * After that the connection isn't rebuild automatically any more and the CannotSendDueNoConnection message
   * is sent for each attempt to talk with the remote actor
   */
  case class ConnectionError(override val lastException: Throwable,
                             override val unsentMessages: Seq[CommunicationMessages.SendMessage],
                             override val connectionManagingActor: ActorRef)
    extends CommunicationError(lastException, unsentMessages, connectionManagingActor)

  /**
   * Is sent when the connection couldn't be established and the system doesn't try to re-establish the connection
   */
  case class CannotSendDueNoConnection(override val lastException: Throwable,
                                       unsentMessage: CommunicationMessages.SendMessage,
                                       override val connectionManagingActor: ActorRef)
    extends CommunicationError(lastException, unsentMessage :: Nil, connectionManagingActor) {

    def trySentGetMessage(): Option[Any] = tryGetMessages.head._1

    def trySentGetSender(): Option[ActorRef] = tryGetMessages.head._2
  }

  /**
   * Is sent when the connection has been closed. Reopen with PleaseTryToReconnect
   */
  case class CannotSendDueClosedConnection(override val lastException: Throwable,
                                           unsentMessage: CommunicationMessages.SendMessage,
                                           override val connectionManagingActor: ActorRef)
    extends CommunicationError(lastException, unsentMessage :: Nil, connectionManagingActor) {

    def trySentGetMessage(): Option[Any] = tryGetMessages.head._1

    def trySentGetSender(): Option[ActorRef] = tryGetMessages.head._2
  }

  /**
   * On the Server this message will be ignored, since it waits passively for connections
   *
   * For restart / reconnect with the remote service. Usually sent after a time-out, an event etc.
   * If the networks stack isn't in a 'no-connection' state this message will be ignored.
   *
   */
  case object PleaseTryToReconnect extends FailureHandlingMessages

  /**
   * Request to close the connection to this server
   */
  case object CloseConnection extends FailureHandlingMessages

  /**
   * Is sent by the system when it start to connecting. For example after a PleaseTryToReconnect.
   *
   * On the client: This message is sent when the system starts to build a connection on the server
   * On the server: This message is sent when a client connects.
   */
  case class StartedConnecting(connectionManagingActor: ActorRef) extends FailureHandlingMessages


  abstract class CommunicationError(val lastException: Throwable,
                                    val unsentMessages: Seq[CommunicationMessages.SendMessage],
                                    val connectionManagingActor: ActorRef) extends FailureHandlingMessages {
    def tryGetMessages: Seq[(Option[Any], Option[ActorRef])] = {
      for (m <- unsentMessages) yield {
        if (m.msg.hasMessage) {
          (Some(OnlyBasicSerialisation.deSerializeMessageContent(m.msg.getMessage)), m.sender)
        } else {
          (None, m.sender)
        }
      }
    }
  }

  private object OnlyBasicSerialisation extends Serialisation {
    def toAddressProtocol() = null

    def deSerializeActorRef(refInfo: RemoteActorRefProtocol,
                            nodeId: Either[ClientId, InetSocketAddress]) = null
  }

}
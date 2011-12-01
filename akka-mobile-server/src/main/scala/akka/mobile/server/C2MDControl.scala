package akka.mobile.server

import akka.mobile.communication.NetworkFailures.CommunicationError
import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.mobile.communication.{CommunicationMessage, ClientId, CommunicationMessages, FailureHandlingMessages}

/**
 * @author roman.stoffel@gamlor.info
 * @since 26.11.11
 */

object C2MDFailures {

  case class CouldNotConnectToC2MD(override val lastException: Throwable,
                                   unsentMessage: CommunicationMessages.SendMessage,
                                   override val connectionManagingActor: ActorRef)
    extends CommunicationError(lastException, unsentMessage :: Nil, connectionManagingActor)

  case class NoC2MDRegistrationForDevice(deviceId: ClientId,
                                         unsentMessage: CommunicationMessages.SendMessage,
                                         override val connectionManagingActor: ActorRef)
    extends CommunicationError(new NoC2MDRegistrationForDeviceException(deviceId), unsentMessage :: Nil, connectionManagingActor)


  case class C2MDServerError(message: String, exception: Option[Throwable])


  class NoC2MDRegistrationForDeviceException(deviceId: ClientId) extends Exception("No google device key is available for " + deviceId)

}

object C2MDCommunication {

  /**
   * Same as SendMessage, but will use C2MD if not connection is available
   */
  case class SendMessageAndUseC2MDFallback(msg: AkkaMobileProtocol, sender: Option[ActorRef])
    extends CommunicationMessage

}
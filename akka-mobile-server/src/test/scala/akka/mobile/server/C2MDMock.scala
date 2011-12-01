package akka.mobile.server

import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.actor.ActorRef
import java.util.concurrent.atomic.AtomicBoolean
import akka.mobile.communication.ClientId
import org.scalatest.Assertions

/**
 * @author roman.stoffel@gamlor.info
 * @since 26.11.11
 */

class C2MDMock extends FallBackPushMessageSender {

  var messageWasSent = false

  def sendRequest(device: ClientId, message: AkkaMobileProtocol, sender: Option[ActorRef], connectionHandlingActor: ActorRef) {
    this.synchronized {
      messageWasSent = true
      this.notifyAll()
    }
  }

  def hasMessageBeenSent =
    this.synchronized {
      messageWasSent
    }


  def expectMessageBeenSent() {
    this.synchronized {
      if (!messageWasSent) {
        this.wait(5000)
      }
      if (!messageWasSent) {
        Assertions.fail("no message has been sent after 5 seconds")
      }
    }
  }

  def canSendC2MDToThisClient(clientId: ClientId) = true
}
package akka.mobile.communication

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.TestProbe
import akka.actor.Actor
import akka.util.duration._
import akka.mobile.communication.NetworkFailures._
import java.io.IOException
import akka.mobile.communication.CommunicationMessages.SendMessage
import akka.mobile.testutils.MockSerialisation

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.11.11
 */

class ResendFailedMessagesSpec extends Spec with ShouldMatchers {
  describe("ResendFailedMessages") {
    it("collects and resends messages") {
      val connectionActor = TestProbe()

      val resender = Actor.actorOf[ResendFailedMessages].start()

      val testMessage = SendMessage(null, null)
      resender ! ConnectionError(new IOException("error"), testMessage :: Nil, connectionActor.ref)

      resender ! StartedConnecting(connectionActor.ref)

      val sentMsg = connectionActor.receiveOne(2.seconds)
      sentMsg should be(testMessage)
    }
    it("keeps order") {
      val connectionActor = TestProbe()

      val resender = Actor.actorOf[ResendFailedMessages].start()

      val msg1 = SendMessage(createMsg(1), null)
      val msg2 = SendMessage(createMsg(2), null)
      val msg3 = SendMessage(createMsg(3), null)
      val msg4 = SendMessage(createMsg(3), null)
      resender ! ConnectionError(new IOException("error"), msg1 :: msg2 :: Nil, connectionActor.ref)
      resender ! CannotSendDueNoConnection(new IOException("error"), msg3, connectionActor.ref)
      resender ! CannotSendDueNoConnection(new IOException("error"), msg4, connectionActor.ref)

      resender ! StartedConnecting(connectionActor.ref)

      connectionActor.receiveOne(2.seconds) should be(msg1)
      connectionActor.receiveOne(2.seconds) should be(msg2)
      connectionActor.receiveOne(2.seconds) should be(msg3)
      connectionActor.receiveOne(2.seconds) should be(msg4)
    }
  }

  def createMsg(content: Any) = {
    val msg = MockSerialisation.messageToActor(Left("mock"), None, content, None);
    MockSerialisation.toWireProtocol(msg)
  }

}
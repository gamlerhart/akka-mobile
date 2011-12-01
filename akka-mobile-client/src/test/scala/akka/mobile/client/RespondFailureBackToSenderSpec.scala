package akka.mobile.client

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.mobile.communication.CommunicationMessages.SendMessage
import akka.mobile.protocol.MobileProtocol
import akka.mobile.communication.RespondFailureBackToSender.FailedToSend
import akka.mobile.testutils.{BlackholeActor, TestDevice}
import akka.mobile.communication._
import akka.mobile.testutils.MockSerialisation._
import akka.testkit.TestProbe
import akka.actor.{ActorRef, Actor}

/**
 * @author roman.stoffel@gamlor.info
 * @since 10.11.11
 */

class RespondFailureBackToSenderSpec extends Spec with ShouldMatchers {

  import akka.util.duration._

  describe("RespondFailureBackToSender") {
    it("should report back the the sender") {
      val sender = TestProbe()
      val toTest = Actor.actorOf[RespondFailureBackToSender].start()
      val msg = toOnWireMsg("Hi", sender.ref)
      val error = new Exception("Oh boy")
      toTest ! NetworkFailures.CannotSendDueNoConnection(error,
        SendMessage(msg, Some(sender.ref)), BlackholeActor())

      sender.expectMsg(FailedToSend("Hi", error))
    }
    it("reports multiple messages back") {
      val sender = TestProbe()
      val toTest = Actor.actorOf[RespondFailureBackToSender].start()
      val msg1 = toOnWireMsg("Hi1", sender.ref)
      val msg2 = toOnWireMsg("Hi2", sender.ref)
      val msg3 = toOnWireMsg("Hi3", sender.ref)
      val error = new Exception("Oh boy")
      toTest ! NetworkFailures.ConnectionError(error,
        SendMessage(msg1, Some(sender.ref)) :: SendMessage(msg2, Some(sender.ref)) :: SendMessage(msg3, Some(sender.ref)) :: Nil,
        BlackholeActor())

      val msgs = sender.receiveN(3)
      msgs.size should be(3)
    }
    it("works with partial information") {
      val sender = TestProbe()
      val toTest = Actor.actorOf[RespondFailureBackToSender].start()
      val msg1 = toOnWireMsg("Hi1", sender.ref)
      val error = new Exception("Oh boy")
      toTest ! NetworkFailures.ConnectionError(error,
        SendMessage(msg1, Some(sender.ref)) :: SendMessage(msg1, None) :: Nil,
        BlackholeActor())

      val msgs = sender.receiveN(1)
      msgs.size should be(1)
    }
  }
  describe("RespondFailureBackToSender in the wild") {
    it("reports messages back") {
      val sender = TestProbe()

      val toTest = Actor.actorOf[RespondFailureBackToSender].start()
      val client = MobileRemoteClient.createClient(TestDevice(),
        socketFactory = a => MockSockets.ThrowOnAccess,
        errorHandler = toTest)

      client.actorFor("non-existing", "non-existing.localhost", 1337).!("Try To Send This")(sender.ref)

      val response = sender.receiveOne(10.seconds).asInstanceOf[FailedToSend]
      response.message should be("Try To Send This")
    }

  }

  private def toOnWireMsg(msg: Any, sender: ActorRef): MobileProtocol.AkkaMobileProtocol = {
    toWireProtocol(messageToActor(Left("actor"), Some(sender), msg, None))
  }

}
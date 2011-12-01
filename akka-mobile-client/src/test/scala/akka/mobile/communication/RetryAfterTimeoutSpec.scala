package akka.mobile.communication

import org.scalatest.Spec
import akka.actor.Actor
import akka.util.duration._
import akka.mobile.communication.NetworkFailures._
import java.io.IOException
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.{TestProbe, TestKit}

/**
 * @author roman.stoffel@gamlor.info
 * @since 11.11.11
 */

class RetryAfterTimeoutSpec extends Spec with ShouldMatchers with TestKit {
  describe("RetryAfterTimeout") {
    it("sents a reset after event") {
      val probe = TestProbe()
      val toTest = Actor.actorOf(new RetryAfterTimeout(2.millisecond)).start()

      toTest ! ConnectionError(new IOException("Oh snap"), Nil, probe.ref)


      probe.expectMsg(PleaseTryToReconnect)
    }
    it("keeps retryingtoSend") {
      val probe = TestProbe()
      val toTest = Actor.actorOf(new RetryAfterTimeout(2.millisecond)).start()

      toTest ! ConnectionError(new IOException("Oh snap"), Nil, probe.ref)


      val msgs = probe.receiveN(5, 1.second)
      msgs.length should be(5)
    }
    it("gets slower with retry") {

      val probe = TestProbe()
      val toTest = Actor.actorOf(new RetryAfterTimeout(10.millisecond, 20000000.0)).start()

      toTest ! ConnectionError(new IOException("Oh snap"), Nil, probe.self)


      probe.expectMsg(PleaseTryToReconnect)

      probe.expectNoMsg(2.seconds)
    }
    it("give up after specified amout of retries") {
      val toTest = Actor.actorOf(new RetryAfterTimeout(2.millisecond, 2, 5)).start()

      toTest ! ConnectionError(new IOException("Oh snap"), Nil, self)

      val msgs = receiveN(5, 1.second)

      expectNoMsg(2.seconds)
    }
    it("stops retrying when it starts connecting") {
      val toTest = Actor.actorOf(new RetryAfterTimeout(1.seconds)).start()

      toTest ! ConnectionError(new IOException("Oh snap"), Nil, self)
      toTest ! StartedConnecting(TestProbe().ref)


      expectNoMsg(2.seconds)
    }
  }
}
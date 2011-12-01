package akka.mobile.communication

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.TestProbe
import akka.util.duration._

/**
 * @author roman.stoffel@gamlor.info
 * @since 17.11.11
 */

class BroadcastActorSpec extends Spec with ShouldMatchers {
  describe("The broadcast-actor") {
    it("sends message to all actors") {
      val actor1 = TestProbe()
      val actor2 = TestProbe()
      val actor3 = TestProbe()

      val broadCaster = BroadcastTo(Seq(actor1.ref, actor2.ref, actor3.ref))

      broadCaster ! "Msg1"
      broadCaster ! "Msg2"

      actor1.receiveOne(1.second) should be("Msg1")
      actor1.receiveOne(1.second) should be("Msg2")
      actor2.receiveOne(1.second) should be("Msg1")
      actor2.receiveOne(1.second) should be("Msg2")
      actor3.receiveOne(1.second) should be("Msg1")
      actor3.receiveOne(1.second) should be("Msg2")
    }
  }

}
package akka.mobile.remote

import org.scalatest.Spec
import akka.testkit.TestKit

/**
 * @author roman.stoffel@gamlor.info
 * @since 29.10.11
 */

class ReceiveMessageActorSpec extends Spec with TestKit with TestMesssageProducer {

  describe("Receiver Actor") {
    it("receives message") {
      val msg = buildMockMsg()

    }

  }

}
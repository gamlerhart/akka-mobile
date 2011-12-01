package akka.mobile.communication

import org.scalatest.Spec
import akka.mobile.protocol.MobileProtocol.UuidProtocol
import org.scalatest.matchers.ShouldMatchers

/**
 * @author roman.stoffel@gamlor.info
 * @since 10.11.11
 */

class PropertiesOfProtobuf extends Spec with ShouldMatchers {
  describe("Protobuf-Messages") {
    it("are not equal for the same values") {
      val msg1 = UuidProtocol.newBuilder().setHigh(1234).setLow(5555).build()
      val msg2 = UuidProtocol.newBuilder().setHigh(1234).setLow(5555).build()

      msg2 should not be (msg1)
    }
  }

}
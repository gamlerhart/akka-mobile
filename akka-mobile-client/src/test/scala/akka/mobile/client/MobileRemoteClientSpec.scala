package akka.mobile.client

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.TestProbe
import akka.mobile.testutils.{MockSerialisation, TestDevice}
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 24.11.11
 */

class MobileRemoteClientSpec extends Spec with ShouldMatchers {

  describe("Mobile remote") {
    it("allows posting messages from outside") {

      val client = MobileRemoteClient.createClient(TestDevice());
      val receiver = TestProbe()
      client.register("echo", receiver.ref)

      val msg = MockSerialisation.messageToActor(Left("echo"), None, "External-Hi", None)

      client.asInstanceOf[InternalOperationsProvider].internalOperationsAccess.postMessage(msg.toByteArray, None)

      receiver.expectMsg("External-Hi")
    }
  }

}
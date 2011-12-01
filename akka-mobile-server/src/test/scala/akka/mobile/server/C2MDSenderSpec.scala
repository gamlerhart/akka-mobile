package akka.mobile.server

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.TestProbe
import akka.util.duration._
import akka.mobile.testutils.{TestDevice, BlackholeActor, TestConfigs}
import akka.mobile.communication.ClientId
import akka.mobile.server.C2MDFailures.NoC2MDRegistrationForDevice

/**
 * @author roman.stoffel@gamlor.info
 * @since 26.11.11
 */

class C2MDSenderSpec extends Spec with ShouldMatchers {

  describe("C2MDSender") {
    it("reports error when it cannots connect") {
      val errorHandler = TestProbe()
      val deviceId = TestDevice().clientId
      val db = new InMemoryClientDatabase()
      db.storeKeyFor(deviceId, "google-key")
      val c2mdSender = new C2MDSender(TestConfigs.defaultServer, db, errorHandler.ref)

      c2mdSender.sendRequest(deviceId, null, None, BlackholeActor())

      val msg = errorHandler.receiveOne(5.seconds)

      msg should not be (null)

      c2mdSender.close()
    }

    it("reports error when device isn't in the database") {
      val errorHandler = TestProbe()
      val c2mdSender = new C2MDSender(TestConfigs.defaultServer, new InMemoryClientDatabase(), errorHandler.ref)

      val clientId = ClientId("cannot find", "me")
      c2mdSender.sendRequest(clientId, null, None, BlackholeActor())

      val msg = errorHandler.receiveOne(5.seconds)

      msg should not be (null)
      msg.asInstanceOf[NoC2MDRegistrationForDevice].deviceId should be(clientId)

      c2mdSender.close()
    }
  }

}
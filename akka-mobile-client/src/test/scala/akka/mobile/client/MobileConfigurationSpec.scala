package akka.mobile.client

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.config.Configuration

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

class MobileConfigurationSpec extends Spec with ShouldMatchers {

  import akka.util.duration._

  val emptyInstance = new MobileConfiguration(
    Configuration.fromResource("emptyTestConfig.conf", getClass.getClassLoader))
  val configuredInstance = new MobileConfiguration(
    Configuration.fromResource("configuredTestConfig.conf", getClass.getClassLoader))


  describe("Empty Default Configuration") {
    it("has not port and host") {
      emptyInstance.HOST should be(None)
      emptyInstance.PORT should be(None)
    }
    it("has a 5 seconds connection timeout") {
      emptyInstance.CONNECT_TIMEOUT should be(5.seconds)
    }
    it("has a 5 retries on connection failures within 10 seconds") {
      emptyInstance.RETRIES_ON_CONNECTION_FAILURES should be(3)
      emptyInstance.RETRIES_TIME_FRAME should be(20.seconds)
    }
    it("has no c2md settings") {
      emptyInstance.C2MD_EMAIL should be(None)
      emptyInstance.C2MD_REGISTRATION_MODE should be("MANUAL")
    }
  }
  describe("Configured Configuration") {
    it("has a port and host") {
      configuredInstance.HOST should be(Some("mock-test.localhost"))
      configuredInstance.PORT should be(Some(1337))
    }
    it("has a 3 seconds connection timeout") {
      configuredInstance.CONNECT_TIMEOUT should be(3.seconds)
    }
    it("has a 3 retries on connection failures within 5 seconds") {
      configuredInstance.RETRIES_ON_CONNECTION_FAILURES should be(3)
      configuredInstance.RETRIES_TIME_FRAME should be(5.seconds)
    }
    it("has c2md settings") {
      configuredInstance.C2MD_EMAIL should be(Some("your-c2md-email@localhost"))
      configuredInstance.C2MD_REGISTRATION_MODE should be("AUTO")
    }
  }

}
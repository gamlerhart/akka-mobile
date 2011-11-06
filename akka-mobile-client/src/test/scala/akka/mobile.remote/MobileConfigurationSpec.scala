package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.config.Configuration

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

class MobileConfigurationSpec extends Spec with ShouldMatchers {

  import akka.util.duration._

  val emptyInstance = new MobileConfiguration(Configuration.fromResource("emptyTestConfig.conf"))
  val configuredInstance = new MobileConfiguration(Configuration.fromResource("configuredTestConfig.conf"))


  describe("Empty Default Configuration") {
    it("has not port and host") {
      emptyInstance.HOST should be(None)
      emptyInstance.PORT should be(None)
    }
    it("has a 5 seconds connection timeout") {
      emptyInstance.CONNECT_TIMEOUT should be(5.seconds)
    }
    it("has a 5 retries on connection failures within 10 seconds") {
      emptyInstance.RETRIES_ON_CONNECTION_FAILURES should be(5)
      emptyInstance.RETRIES_TIME_FRAME should be(10.seconds)
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
  }

}
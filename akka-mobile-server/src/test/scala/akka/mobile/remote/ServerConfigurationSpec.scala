package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.config.Configuration

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

class ServerConfigurationSpec extends Spec with ShouldMatchers {
  val emptyInstance = new ServerConfiguration(Configuration.fromResource("serverEmptyConfig.conf"))
  val configuredInstance = new ServerConfiguration(Configuration.fromResource("serverConfiguredConfig.conf"))


  describe("Empty Default Configuration") {
    it("has not port and host") {
      emptyInstance.HOST should be(None)
      emptyInstance.PORT should be(None)
    }
    it("has backlog of 4096") {
      emptyInstance.BACKLOG should be(4096)
    }
  }
  describe("Configured Configuration") {
    it("has not port and host") {
      configuredInstance.HOST should be(Some("0.0.0.0"))
      configuredInstance.PORT should be(Some(1337))
    }
    it("has backlog of 1024") {
      configuredInstance.BACKLOG should be(1024)
    }
  }

}
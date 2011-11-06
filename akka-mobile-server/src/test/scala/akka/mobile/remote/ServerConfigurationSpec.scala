package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.config.Configuration

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

class ServerConfigurationSpec extends Spec with ShouldMatchers {

  import akka.util.duration._

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
    it("pool config") {
      emptyInstance.EXECUTION_POOL_SIZE should be(16)
      emptyInstance.MAX_CHANNEL_MEMORY_SIZE should be(0)
      emptyInstance.MAX_TOTAL_MEMORY_SIZE should be(0)
      emptyInstance.EXECUTION_POOL_KEEPALIVE should be(60.seconds)
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
    it("pool config") {
      configuredInstance.EXECUTION_POOL_SIZE should be(32)
      configuredInstance.MAX_CHANNEL_MEMORY_SIZE should be(1024)
      configuredInstance.MAX_TOTAL_MEMORY_SIZE should be(1024)
      configuredInstance.EXECUTION_POOL_KEEPALIVE should be(30.seconds)
    }
  }

}
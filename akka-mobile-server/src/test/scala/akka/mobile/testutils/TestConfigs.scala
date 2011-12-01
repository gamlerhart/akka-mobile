package akka.mobile.testutils

import akka.mobile.server.ServerConfiguration
import akka.mobile.client.MobileConfiguration

/**
 * @author roman.stoffel@gamlor.info
 * @since 24.11.11
 */

object TestConfigs {
  val defaultServer = ServerConfiguration.fromResource("defaultServerTestConfig.conf")
  val defaultClient = MobileConfiguration.fromResource("defaultClientTestConfig.conf")
  val autoRegisterC2MDConf = MobileConfiguration.fromResource("autoRegisterC2MD.conf")

}
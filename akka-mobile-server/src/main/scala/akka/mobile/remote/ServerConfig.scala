package akka.mobile.remote

import akka.config.Configuration

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

object ServerConfig {

}

class ServerConfig(akkaConfig: Configuration) extends SharedConfig(akkaConfig) {

  val PORT = akkaConfig.getInt("akka.mobile.server.port")
  val HOST = akkaConfig.getString("akka.mobile.server.host")
}
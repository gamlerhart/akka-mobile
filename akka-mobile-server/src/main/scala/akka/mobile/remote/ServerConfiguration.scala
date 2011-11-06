package akka.mobile.remote

import akka.config.{Config, Configuration}


/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

object ServerConfiguration {
  val defaultConfig = new ServerConfiguration(Config.config)

}

class ServerConfiguration(akkaConfig: Configuration) extends SharedConfig(akkaConfig) {

  val PORT = akkaConfig.getInt("akka.mobile.server.port")
  val HOST = akkaConfig.getString("akka.mobile.server.host")


  val BACKLOG = akkaConfig.getInt("akka.mobile.server.backlog", 4096)
}
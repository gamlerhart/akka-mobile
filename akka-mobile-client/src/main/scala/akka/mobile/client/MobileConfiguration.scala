package akka.mobile.client

import akka.config.{Configuration, Config}
import akka.util.Duration
import akka.mobile.SharedConfig


/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class MobileConfiguration(akkaConfig: Configuration) extends SharedConfig(akkaConfig) {


  val PORT = akkaConfig.getInt("akka.mobile.client.port")
  val HOST = akkaConfig.getString("akka.mobile.client.host")

  val CONNECT_TIMEOUT = Duration(akkaConfig.getLong("akka.mobile.client.connecttimeout", 5), TIME_UNIT)

  val RETRIES_ON_CONNECTION_FAILURES = akkaConfig.getInt("akka.mobile.retriesOnConnectionFailure", 3)
  val RETRIES_TIME_FRAME = Duration(akkaConfig.getLong("akka.mobile.retryTimout", 20), TIME_UNIT)


  val C2MD_EMAIL = akkaConfig.getString("akka.mobile.c2md.email")
  val C2MD_REGISTRATION_MODE = akkaConfig.getString("akka.mobile.c2md.register-mode", "MANUAL")
}


object MobileConfiguration {
  val defaultConfig = new MobileConfiguration(Config.config)


  def fromResource(resource: String) = new MobileConfiguration(
    Configuration.fromResource(resource, getClass.getClassLoader))
}
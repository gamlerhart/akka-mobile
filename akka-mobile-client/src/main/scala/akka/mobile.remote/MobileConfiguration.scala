package akka.mobile.remote

import akka.config.{Configuration, Config}
import akka.util.Duration


/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class MobileConfiguration(akkaConfig: Configuration) extends SharedConfig(akkaConfig) {


  val PORT = akkaConfig.getInt("akka.mobile.client.port")
  val HOST = akkaConfig.getString("akka.mobile.client.host")

  val CONNECT_TIMEOUT = Duration(akkaConfig.getLong("akka.mobile.client.connecttimeout", 5), TIME_UNIT)

  val RETRIES_ON_CONNECTION_FAILURES = akkaConfig.getInt("akka.mobile.retriesOnConnectionFailure", 5)
  val RETRIES_TIME_FRAME = Duration(akkaConfig.getLong("akka.mobile.retryTimout", 10), TIME_UNIT)
}

class SharedConfig(akkaConfig: Configuration) {
  val TIME_UNIT = akkaConfig.getString("akka.time-unit", "seconds")


}

object MobileConfiguration {
  val defaultConfig = new MobileConfiguration(Config.config)
}
package akka.mobile.remote

import akka.config.{Configuration, Config}


/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class MobileConfiguration(akkaConfig: Configuration) {
  val DEVICE_INTERFACE_CLASS = akkaConfig.getString("akka.mobile.device.interface")


}

object MobileConfiguration {
  val defaultConfig = new MobileConfiguration(Config.config)
}
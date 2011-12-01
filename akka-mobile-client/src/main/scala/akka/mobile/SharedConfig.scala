package akka.mobile

import akka.config.Configuration


/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class SharedConfig(akkaConfig: Configuration) {
  val TIME_UNIT = akkaConfig.getString("akka.time-unit", "seconds")


}
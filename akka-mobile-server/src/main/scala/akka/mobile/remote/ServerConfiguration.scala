package akka.mobile.remote

import akka.config.{Config, Configuration}
import akka.util.Duration


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

  val EXECUTION_POOL_SIZE = {
    val sz = akkaConfig.getInt("akka.mobile.server.execution-pool-size", 16)
    if (sz < 1) throw new IllegalArgumentException("akka.mobile.server.execution-pool-size is less than 1")
    sz
  }
  val MAX_CHANNEL_MEMORY_SIZE = {
    val sz = akkaConfig.getInt("akka.mobile.server.max-channel-memory-size", 0)
    if (sz < 0) throw new IllegalArgumentException("akka.mobile.server.max-channel-memory-size is less than 0")
    sz
  }
  val MAX_TOTAL_MEMORY_SIZE = {
    val sz = akkaConfig.getInt("akka.mobile.server.max-total-memory-size", 0)
    if (sz < 0) throw new IllegalArgumentException("akka.mobile.server.max-total-memory-size is less than 0")
    sz
  }
  val EXECUTION_POOL_KEEPALIVE = Duration(akkaConfig.getInt("akka.mobile.server.execution-pool-keepalive", 60), TIME_UNIT)
}
package akka.mobile.testutils

import java.net.ServerSocket

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

object NetworkUtils {

  def findFreePort() = {
    val tester = new ServerSocket(0)
    val port = tester.getLocalPort
    tester.close()
    port
  }

}
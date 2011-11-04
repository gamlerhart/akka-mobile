package akka.mobile.testutils

import akka.mobile.remote.{ClientId, DeviceOperations}


/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

object TestDevice extends DeviceOperations {
  val clientId = ClientId("test-device", "test-app")
}
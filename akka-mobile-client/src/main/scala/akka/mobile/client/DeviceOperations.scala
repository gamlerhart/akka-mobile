package akka.mobile.client

import akka.mobile.communication.ClientId

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

/**
 * The interface behind which we hide the gritty details of Android =)
 */
trait DeviceOperations {
  def clientId: ClientId

  def getPreference(key: String, defaultValue: String): String

  def getPreference(key: String, defaultValue: Int): Int

  def storePreference(writeClosure: PropertyWriteContext => Unit)

  def registerForC2MD(forEmail: String): Unit

  trait PropertyWriteContext {
    def put(key: String, valueToSet: String)

    def put(key: String, valueToSet: Int)

  }

}

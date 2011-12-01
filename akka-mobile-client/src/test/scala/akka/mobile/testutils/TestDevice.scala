package akka.mobile.testutils

import akka.mobile.communication.ClientId
import akka.mobile.client.DeviceOperations
import java.util.concurrent.atomic.AtomicBoolean
import java.util.UUID


/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class TestDevice(val clientId: ClientId = TestDevice.clientId) extends DeviceOperations {
  var storage = scala.collection.mutable.Map[String, Any]()
  val requstrationRequestSent = new AtomicBoolean(false)

  def getPreference(key: String, defaultValue: String): String = {
    readValue(key, defaultValue)
  }

  def getPreference(key: String, defaultValue: Int): Int = {
    readValue(key, defaultValue)
  }


  def registerForC2MD(forEmail: String) {
    requstrationRequestSent.set(true)
  }

  def hasRegisteredForC2MD = requstrationRequestSent.get()

  def storePreference(writeClosure: (PropertyWriteContext) => Unit) = {
    writeClosure(new PropertyWriteContext() {
      def put(key: String, valueToSet: String) {
        store(key, valueToSet)
      }

      def put(key: String, valueToSet: Int) {
        store(key, valueToSet)

      }

      def store(key: String, value: Any) {
        storage.synchronized {
          storage.put(key, value)
        }
      }
    })
  }

  private def readValue[T](key: String, defaultValue: T): T = {
    storage.synchronized {
      storage.get(key) match {
        case Some(x) => x.asInstanceOf[T]
        case None => defaultValue
      };
    }
  }
}

object TestDevice {
  val clientId = ClientId("test-device", "test-app")

  def apply() = new TestDevice()

  def uniqueTestDevice() = new TestDevice(ClientId(UUID.randomUUID().toString, "test-app"))
}
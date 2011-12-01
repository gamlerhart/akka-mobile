package akka.mobile.testsuite

import android.os.Bundle
import akka.actor.ActorRef
import java.lang.{UnsupportedOperationException, String}
import java.net.InetSocketAddress
import akka.mobile.client.{MobileConfiguration, InternalOperations, InternalOperationsProvider, RemoteClient}
import junit.framework.{Assert, TestCase}
import android.test.InstrumentationTestCase
import android.content.{Context, Intent, BroadcastReceiver}
import akka.mobile.android.{C2MDException, C2MDReceiver}
import android.util.Base64

/**
 * @author roman.stoffel@gamlor.info
 * @since 24.11.11
 */

class C2MDReceiverTest extends TestCase {
  val RegisterAction = "com.google.android.c2dm.intent.REGISTRATION"
  val ReceiveAction = "com.google.android.c2dm.intent.RECEIVE"

  def testDispatchesC2MDSend() {
    val intent: Intent = buildSendIntent

    val client = new ClientMock()
    val toTest = new MockReceiver(client);


    toTest.onReceive(null, intent)

    client.assertMessage((m, s) => s.isDefined)
    client.assertMessage((m, s) => m.length == 1)
  }

  def testDispatchesC2MDRegister() {
    val intent: Intent = buildRegisterIntent

    val client = new ClientMock()
    val toTest = new MockReceiver(client);


    toTest.onReceive(null, intent)

    client.assertRegistration((m, s) => m == "test-id-42")
  }

  def testThrowsOnError() {
    val intent: Intent = buildIntent(RegisterAction, Map("error" -> "Oups"))

    val client = new ClientMock()
    val toTest = new MockReceiver(client);

    try {
      toTest.onReceive(null, intent)
      Assert.fail("ExpectedException")
    } catch {
      case C2MDException("Oups") => {
        // Expected
      }
      case x => {
        throw x
      }
    }

  }


  private def buildSendIntent: Intent = {
    val intent = new Intent(ReceiveAction)
    val data = new Bundle();
    data.putString("payload", Base64.encodeToString(Array(1.toByte), Base64.DEFAULT));
    intent.putExtras(data)
    intent
  }

  private def buildRegisterIntent(): Intent
  = buildIntent(RegisterAction, Map("registration_id" -> "test-id-42"))

  private def buildIntent(action: String, values: Map[String, String]) = {
    val intent = new Intent(action)
    val data = new Bundle();
    values.foreach(kv => {
      data.putString(kv._1, kv._2)
    })
    intent.putExtras(data)
    intent
  }


  class MockReceiver(client: RemoteClient) extends C2MDReceiver {
    def remoteClient(context: Context) = client

    override def serverAddress(context: Context) = {
      Some(("mock.localhost", 42))
    }
  }

  class ClientMock(val configuration: MobileConfiguration
                   = MobileConfiguration.defaultConfig)
    extends RemoteClient with InternalOperationsProvider with InternalOperations {

    var postedMessage: Option[(Array[Byte], Option[InetSocketAddress])] = None
    var registrationRequest: Option[(String, InetSocketAddress)] = None

    def connectNow(hostname: String, port: Int) {
      notAvailable()
    }

    def actorFor(serviceId: String, hostname: String, port: Int) = notAvailable()

    def actorFor(serviceId: String) = notAvailable()

    def register(idOfActor: String, actorRef: ActorRef) {
      notAvailable()
    }

    def internalOperationsAccess = this


    def postMessage(messageBytes: Array[Byte], server: Option[InetSocketAddress]) {
      postedMessage = Some((messageBytes, server))
    }


    def registerDevice(registrationKey: String, server: InetSocketAddress) {
      registrationRequest = Some((registrationKey, server))
    }


    def c2mdRegistrationKey = None


    def requestC2MDRegistration() {
      notAvailable()
    }

    def notAvailable[T](): T = {
      throw new UnsupportedOperationException("Test-Instance")
    }

    def assertMessage(msgAssert: (Array[Byte], Option[InetSocketAddress]) => Boolean) {
      if (postedMessage.isEmpty) {
        Assert.fail("No message has been posted =(")
      } else {
        val p = postedMessage.get
        Assert.assertTrue(msgAssert(p._1, p._2))
      }
    }

    def assertRegistration(msgAssert: (String, InetSocketAddress) => Boolean) {
      if (registrationRequest.isEmpty) {
        Assert.fail("No registration has been requested =(")
      } else {
        val p = registrationRequest.get
        Assert.assertTrue(msgAssert(p._1, p._2))
      }
    }

  }

}
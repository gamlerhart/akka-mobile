package info.gamlor.remoting

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.TestProbe
import akka.mobile.server.ServerEvents.{NewC2MDRegistration, ClientConnected}
import akka.actor.Actor
import org.mockito.Mockito._
import org.mockito.Matchers._
import java.net.InetSocketAddress
import akka.mobile.communication.RemoteMessages.RegisterMeForC2MD._
import akka.mobile.communication.RemoteMessages.RegisterMeForC2MD
import akka.mobile.client.{MobileRemoteClient, RemoteClient, C2MDRegisterProcess, InternalOperationsProvider}
import akka.mobile.testutils.{TestConfigs, BlackholeActor, TestDevice, TestServer}

/**
 * @author roman.stoffel@gamlor.info
 * @since 24.11.11
 */

class ClientRegistersC2MDSpec extends Spec with ShouldMatchers {

  describe("The client") {
    it("registers C2MD on the server") {
      TestServer.withRunningServer(ctx => {
        val client = ctx.connectAClient()
        val listenOnServer = TestProbe()
        ctx.server.addListener(listenOnServer.ref);

        val internalOperations = client.asInstanceOf[InternalOperationsProvider].internalOperationsAccess
        internalOperations.registerDevice("c2md-key")

        listenOnServer.ignoreMsg({
          case ClientConnected(_) => true
        })

        listenOnServer.expectMsg(NewC2MDRegistration(TestDevice.clientId, "c2md-key"))

      })
    }
    it("knows its registration state") {
      TestServer.withRunningServer(ctx => {
        val client = ctx.connectAClient()
        client.c2mdRegistrationKey should be(None)


        val internalOperations = client.asInstanceOf[InternalOperationsProvider].internalOperationsAccess
        internalOperations.registerDevice("c2md-key")

        while (client.c2mdRegistrationKey == None) {
          Thread.sleep(100)
        }
        client.c2mdRegistrationKey should be(Some("c2md-key"))
      })
    }
    it("sends registration request intent") {
      val testDevice = TestDevice()
      MobileRemoteClient.createClient(testDevice,
        configuration = TestConfigs.defaultClient).requestC2MDRegistration()
      testDevice.hasRegisteredForC2MD should be(true)
    }
    it("sends registration request intent automatically") {
      val testDevice = TestDevice()
      val client = MobileRemoteClient.createClient(testDevice,
        configuration = TestConfigs.autoRegisterC2MDConf)
      var timeOut = 5000
      while (!testDevice.hasRegisteredForC2MD && timeOut > 0) {
        Thread.sleep(100)
        timeOut -= 100
      }
      testDevice.hasRegisteredForC2MD should be(true)
    }
  }

  describe("The C2MDRegisterProcess") {
    it("stores started state") {
      val device = TestDevice()
      val mockClient = mock(classOf[RemoteClient])
      when(mockClient.actorFor(anyObject(), anyObject(), anyObject())).thenReturn(BlackholeActor())
      when(mockClient.configuration).thenReturn(TestConfigs.defaultClient)
      val process = Actor.actorOf(new C2MDRegisterProcess(device, mockClient)).start()

      process ! C2MDRegisterProcess.RegisterWith("c2md-key")

      (process ? C2MDRegisterProcess.IsRegisteredRequest).await

      device.getPreference("last-given-google-c2md-key", "") should be("c2md-key")
      device.getPreference("google-c2md-registration-host", "") should be("test.localhost")
      device.getPreference("google-c2md-registration-port", 0) should be(42)
    }
    it("resumes when a last key not equal to registered key") {
      val device = TestDevice()
      device.storePreference(wc => {
        wc.put("last-given-google-c2md-key", "new-key")
        wc.put("registered-c2md-key", "old-key")
        wc.put("google-c2md-registration-host", "test.localhost")
        wc.put("google-c2md-registration-port", 42)
      })
      val serverSideRegistration = TestProbe()
      val mockClient = mock(classOf[RemoteClient])
      when(mockClient.actorFor(anyObject(), anyObject(), anyObject())).thenReturn(serverSideRegistration.ref)
      when(mockClient.configuration).thenReturn(TestConfigs.defaultClient)
      Actor.actorOf(new C2MDRegisterProcess(device, mockClient)).start()

      serverSideRegistration.expectMsg(RegisterMeForC2MD(device.clientId, "new-key"))
    }
  }

}
package akka.mobile.server

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.mobile.communication.ClientId
import akka.testkit.TestProbe
import akka.util.duration._
import akka.mobile.testutils.{TestConfigs, TestServer}
import akka.mobile.client.InternalOperationsProvider
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 01.12.11
 */

class ClientStateSpec extends Spec with ShouldMatchers {

  describe("The Remote Server") {
    it("cannot know unknown client") {
      TestServer.withRunningServer(ctx => {
        ctx.server.connectionStateOf(ClientId("unknown", "client")) should be(NotConnectedNoC2MD)
      })
    }
    it("connected") {
      TestServer.withRunningServer(ctx => {
        val waitForConnection = TestProbe()
        ctx.server.addListener(waitForConnection.ref)
        val client = ctx.connectAClient()
        client.connectNow()
        waitForConnection.receiveOne(5.seconds)
        ctx.server.connectionStateOf(client.clientId) should be(Connected)
      })
    }
    it("c2md available") {
      TestServer.withRunningServer(ctx => {
        val waitForConnection = TestProbe()
        ctx.server.addListener(waitForConnection.ref)
        val client = ctx.connectAClient(TestConfigs.defaultClient)
        client.asInstanceOf[InternalOperationsProvider]
          .internalOperationsAccess.registerDevice("key", new InetSocketAddress("localhost", ctx.port))
        waitForConnection.receiveOne(5.seconds)

        client.closeConnections()

        ctx.server.connectionStateOf(client.clientId) should be(NotConnectedC2MDAvailable)
      })
    }

  }

}
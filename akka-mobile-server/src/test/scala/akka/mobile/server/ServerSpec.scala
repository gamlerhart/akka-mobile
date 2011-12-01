package akka.mobile.server

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.mobile.client.InternalOperationsProvider
import java.net.InetSocketAddress
import akka.mobile.testutils.{TestDevice, TestServer, TestConfigs}

/**
 * @author roman.stoffel@gamlor.info
 * @since 26.11.11
 */

class ServerSpec extends Spec with ShouldMatchers {

  describe("The server") {
    it("should fill the database with C2MD keys") {
      TestServer.withRunningServer(ctx => {
        val echoname = ctx.registerStandardEchoActor()
        val client = ctx.connectAClient()
        val internals = client.asInstanceOf[InternalOperationsProvider].internalOperationsAccess

        internals.registerDevice("the-key", new InetSocketAddress("localhost", ctx.port))


        (client.actorFor(echoname) ? "Hi").await

        val value = ctx.database.apiKeyFor(client.clientId)
        value should be(Some("the-key"))
      })

    }
  }

}
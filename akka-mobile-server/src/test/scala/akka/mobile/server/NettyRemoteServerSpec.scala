package akka.mobile.server

import org.scalatest.Spec
import akka.actor.Actor
import java.net.Socket
import java.io.IOException
import akka.mobile.client.{MobileRemoteClient, EchoActor}
import akka.mobile.testutils.{TestDevice, TestServer, NetworkUtils}
import akka.testkit.TestProbe
import akka.mobile.server.ServerEvents.ClientConnected
import org.scalatest.matchers.ShouldMatchers

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

class NettyRemoteServerSpec extends Spec with ShouldMatchers {

  describe("The Remote Server") {
    it("cannot be used after shutting down") {
      val server = NettyRemoteServer.start("localhost", NetworkUtils.findFreePort())
      server.shutdownServerModule()

      intercept[IllegalStateException] {
        server.register("my-id", Actor.actorOf[EchoActor])
      }
    }

    it("opens port on which it can be reached") {
      val port = NetworkUtils.findFreePort();
      val server = NettyRemoteServer.start("localhost", port)

      val socket = new Socket("localhost", port)
      socket.getInputStream
      socket.close()

      server.shutdownServerModule()
    }
    it("closes port on shutdown") {
      val port = NetworkUtils.findFreePort();
      val server = NettyRemoteServer.start("localhost", port)
      server.shutdownServerModule()

      intercept[IOException] {
        val socket = new Socket("localhost", port)
      }
    }
    it("clients can connect") {
      TestServer.withRunningServer(ctx => {
        val echoActor = ctx.registerStandardEchoActor()
        (ctx.connectAClient().actorFor(echoActor) ? "Hi-1").get should be("Hi-1")
        (ctx.connectAClient().actorFor(echoActor) ? "Hi-2").get should be("Hi-2")
        (ctx.connectAClient().actorFor(echoActor) ? "Hi-3").get should be("Hi-3")
      })
    }
    it("notifies on connection") {
      TestServer.withRunningServer(ctx => {
        val eventListener = TestProbe()
        ctx.server.addListener(eventListener.ref)
        val client = MobileRemoteClient.createClient(TestDevice(), "localhost", ctx.port)
        val echoActorName = ctx.registerStandardEchoActor()
        client.actorFor(echoActorName) ! "Hi"

        eventListener.expectMsg(ClientConnected(TestDevice.clientId))
      })
    }

  }

}
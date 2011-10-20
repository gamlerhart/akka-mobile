package akka.mobile.remote

import org.scalatest.Spec
import akka.actor.Actor
import akka.mobile.testutils.NetworkUtils
import java.net.Socket
import java.io.IOException

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

class NettyRemoteServerSpec extends Spec{

  describe("The Remote Server") {
    it("cannot be used after shutting down"){
      val server = NettyRemoteServer.start("localhost",NetworkUtils.findFreePort())
      server.shutdownServerModule()

      intercept[IllegalStateException] {
        server.register("my-id",Actor.actorOf[EchoActor])
      }
    }

    it("opens port on which it can be reached"){
      val port = NetworkUtils.findFreePort();
      val server = NettyRemoteServer.start("localhost",port)

      val socket = new Socket("localhost",port)
      socket.getInputStream
      socket.close()

      server.shutdownServerModule()
    }
    it("closes port on shutdown"){
      val port = NetworkUtils.findFreePort();
      val server = NettyRemoteServer.start("localhost",port)
      server.shutdownServerModule()

      intercept[IOException] {
        val socket = new Socket("localhost",port)
      }
    }

  }

}
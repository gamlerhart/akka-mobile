package info.gamlor.remoting

import akka.actor.ActorRef
import akka.mobile.remote.{RemoteServer, NettyRemoteServer}
import akka.mobile.testutils.NetworkUtils

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

object TestServer {

  def withRunningServer(toRun: TestServerContext => Unit) {
    val port = NetworkUtils.findFreePort()
    val server = NettyRemoteServer.start("152.96.235.59", port);
    toRun(TestServerContext(server, port))
    server.shutdownServerModule();
  }

}

case class TestServerContext(server: RemoteServer, port: Int) {

  def register(id: String, actor: ActorRef) {
    server.register(id, actor)
  }
}
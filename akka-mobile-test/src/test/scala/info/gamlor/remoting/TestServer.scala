package info.gamlor.remoting

import akka.event.EventHandler
import akka.remoteinterface.{RemoteSupport, RemoteModule}
import akka.actor.ActorRef
import akka.mobile.remote.{MobileRemoteServer, NettyRemoteServer}
import akka.mobile.testutils.NetworkUtils

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

object TestServer {

  def withRunningServer(toRun : TestServerContext=>Unit){
    val port = NetworkUtils.findFreePort()
    val server = NettyRemoteServer.start("localhost",port);
    toRun(TestServerContext(server,port))
    server.shutdownServerModule();
  }

}

case class TestServerContext(server : MobileRemoteServer, port : Int)  {

  def register(id:String,actor: ActorRef) {
    server.register(id, actor)
  }
}
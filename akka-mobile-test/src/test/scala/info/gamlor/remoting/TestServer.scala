package info.gamlor.remoting

import akka.event.EventHandler
import akka.remoteinterface.{RemoteSupport, RemoteModule}
import akka.actor.ActorRef
import akka.mobile.remote.{MobileRemoteServer, NettyRemoteServer}

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

object TestServer {
  val PORT = 2552

  def withRunningServer(toRun : TestServerContext=>Unit){
    val server = NettyRemoteServer.start("localhost",PORT);
    toRun(TestServerContext(server,PORT))
    server.shutdownServerModule();
  }

}

case class TestServerContext(server : MobileRemoteServer, port : Int)  {

  def register(id:String,actor: ActorRef) {
    server.register(id, actor)
  }
}
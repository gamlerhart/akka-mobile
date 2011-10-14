package info.gamlor.remoting

import akka.remote.netty.{NettyRemoteServerModule, NettyRemoteServer, NettyRemoteSupport}
import akka.event.EventHandler
import akka.remoteinterface.{RemoteSupport, RemoteModule}
import akka.actor.ActorRef

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

object TestServer {
  val PORT = 2552

  def withRunningServer(toRun : TestServerContext=>Unit){
    val server = new NettyRemoteSupport {

      override def optimizeLocalScoped_?() = false
    }
    server.start("localhost",PORT);
    toRun(TestServerContext(server,PORT))
    server.shutdownServerModule();
  }

}

case class TestServerContext(server : RemoteSupport, port : Int)  {

  def register(id:String,actor: ActorRef) {
    server.register(id, actor)
  }
}
package info.gamlor.play

import akka.remote.netty.{NettyRemoteServerModule, NettyRemoteServer, NettyRemoteSupport}

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

object TestServer {

  def withRunningServer(toRun : ()=>Unit){
    val server = new NettyRemoteServerModule {
      protected[akka] def notifyListeners(message: => Any) {}

      def optimizeLocalScoped_?() = null
    }
  }

}
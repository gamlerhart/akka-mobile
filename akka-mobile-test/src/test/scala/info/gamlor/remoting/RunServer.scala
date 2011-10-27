package info.gamlor.remoting

import akka.mobile.remote.NettyRemoteServer
import akka.actor.Actor
import info.gamlor.remoting.ReceiveCheckActor
import org.scalatest.Spec

/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */

class RunServer extends Spec {

  describe("Test Server"){
    it("is running"){
  val server = NettyRemoteServer.start("localhost", 2552);


  val local = Actor.actorOf(new ReceiveCheckActor(None)).start();
  server.register("echo", local);

    }
  }
}
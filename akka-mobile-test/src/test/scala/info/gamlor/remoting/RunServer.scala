package info.gamlor.remoting

import org.scalatest.Spec
import akka.config.Supervision.OneForOneStrategy
import akka.actor.Actor

/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */
//
//class RunServer extends Spec {
//
//  describe("Test Server") {
//    it("is running") {
//      val server = NettyRemoteServer.start("localhost", 2552);
//
//
//      val local = Actor.actorOf(new ReceiveCheckActor(None)).start();
//      server.register("echo", local);
//
//    }
//  }
//}
//
class PlayAround extends Spec {

  class KilledMyselfException extends Exception

  describe("Test Server") {
    it("dum di dum") {


      val neverDone = Actor.actorOf(new Actor() {
        self.faultHandler = OneForOneStrategy(List(classOf[Exception]), 1, 5000)

        override def preStart() {
          println("start")
        }

        override def preRestart(reason: Throwable, message: Option[Any]) {
          println("restart")
        }

        protected def receive = {
          case "FirstMsg" => {
            Thread.sleep(7000)
          }
          case "Fail" => {
            println("Super-visor?" + self.supervisor.isDefined)
            throw new KilledMyselfException()
          }
          case "Fun" => println("Fun yeah")
          case "Fun 2" => println("Fun yeah2")
        }
      }).start()

      neverDone ! "Fail"
      neverDone ! "Fail"
      neverDone ! "Fail"
      neverDone ! "Fail"
      neverDone ! "Fun"
      neverDone ! "EndMe"
      neverDone ! "Fun 2"
    }
  }

}



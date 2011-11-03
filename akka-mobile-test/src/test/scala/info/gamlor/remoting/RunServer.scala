package info.gamlor.remoting

import org.scalatest.Spec
import akka.config.Supervision.{SupervisorConfig, OneForOneStrategy}
import akka.actor.{ActorRef, Supervisor, Actor}

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


      class ReceiverActor extends Actor() {
        override def preStart() {
          println("child-start")
        }

        override def preRestart(reason: Throwable, message: Option[Any]) {
          println("child-restart")
        }

        protected def receive = {
          case "Fail" => {
            throw new KilledMyselfException()
          }
        }

        override def postStop() {
          println("stop")
        }
      }


      val root = Actor.actorOf(new Actor() {

        private var childActor: ActorRef = null;

        override def preStart() {
          println("SuperVisor")
          childActor = Actor.actorOf(new ReceiverActor())
          self.link(childActor)
          childActor.start()
          self ! "Boot"
        }

        override def preRestart(reason: Throwable, message: Option[Any]) {
          println("parent-restart")
        }

        protected def receive = {
          case "Boot" => {
            childActor ! "Fail"

          }
          case x => println(x)
        }
      })
      val supervisor = Supervisor(SupervisorConfig(
        OneForOneStrategy(List(classOf[Exception]), 2, 10000), Nil
      ))

      supervisor.link(root);
      root.start()

    }
  }

}



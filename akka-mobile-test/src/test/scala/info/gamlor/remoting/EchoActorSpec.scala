package info.gamlor.remoting

import org.scalatest.{WordSpec, Spec}
import akka.testkit.TestKit
import akka.actor.Actor
import TestServer._
import java.util.concurrent.{TimeUnit, CountDownLatch}
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import org.scalatest.matchers.ShouldMatchers._

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

class EchoActorSpec extends WordSpec with ShouldMatchers with TestKit {


  "The Actor must " must {

    "receives message " in {

      withRunningServer(ctx => {
        val barrier = new CountDownLatch(1);
        val local = Actor.actorOf(new EchoActor(Some(barrier))).start();
        ctx.register("echo", local);
        val echo = Actor.remote.actorFor("echo", "localhost", ctx.port);
        echo ! "Hello-Receive-Only"


        val receivedMsg = barrier.await(6,TimeUnit.SECONDS)
        receivedMsg should be( true )
      })
    }

    "be able to reply " in {

      withRunningServer(ctx => {
        val local = Actor.actorOf(new EchoActor(None)).start();
        ctx.register("echo", local);
        val echo = Actor.remote.actorFor("echo", "localhost", ctx.port);
        echo ! "Ask"


        expectMsg("Answer for Ask")
      })
    }

  }

}

class EchoActor(barrier : Option[CountDownLatch] = null) extends Actor {
  protected def receive = {

    case "Hello-Receive-Only" => {
      barrier.get.countDown()
    }
    case x: String => {
      self.reply("Answer for "+x)
    }

  }
}

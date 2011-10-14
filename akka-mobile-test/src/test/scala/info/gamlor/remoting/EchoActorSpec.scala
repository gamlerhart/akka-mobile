package info.gamlor.remoting

import org.scalatest.matchers.MustMatchers
import org.scalatest.{WordSpec, Spec}
import akka.testkit.TestKit
import akka.actor.Actor
import TestServer._

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

class EchoActorSpec extends WordSpec with MustMatchers with TestKit {


  "The Actor must " must {

    "send back a Hello " in {

      withRunningServer(ctx => {
        ctx.register("echo", Actor.actorOf[EchoActor]);
        val echo = Actor.remote.actorFor("echo", "localhost", ctx.port);
        echo ! "Hello"
        expectMsg("Hello")
      })
    }

  }

}

class EchoActor extends Actor {
  protected def receive = {

    case x: String => {
      self.reply(x)
    }

  }
}

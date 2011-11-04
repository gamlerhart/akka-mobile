package info.gamlor.remoting

import org.scalatest.WordSpec
import akka.testkit.TestKit
import TestServer._
import org.scalatest.matchers.ShouldMatchers
import akka.actor.{ActorRef, Actor}
import java.util.concurrent.{CountDownLatch, TimeUnit}
import akka.mobile.remote.MobileRemoteClient
import akka.mobile.testutils.TestDevice

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

class EchoActorSpec extends WordSpec with ShouldMatchers with TestKit {

  val client = MobileRemoteClient.createClient(TestDevice)
  "The Actor must " must {

    "receives message " in {

      withRunningServer(ctx => {
        val barrier = new CountDownLatch(1)
        val local = Actor.actorOf(new ReceiveCheckActor(Some(barrier))).start()
        ctx.register("echo", local)
        val echo = client.actorFor("echo", "localhost", ctx.port)
        echo ! "Hello-Receive-Only"


        val receivedMsg = barrier.await(6, TimeUnit.SECONDS)
        receivedMsg should be(true)
      })
    }

    "be able to reply " in {

      withRunningServer(ctx => {
        val local = Actor.actorOf(new ReceiveCheckActor(None)).start()
        ctx.register("echo", local)
        val echo = client.actorFor("echo", "localhost", ctx.port)
        echo ! "Ask"


        expectMsg("Answer for Ask")
      })
    }
    "can play ping pong " in {

      withRunningServer(ctx => {
        val local = Actor.actorOf(new ReceiveCheckActor(None)).start()
        ctx.register("echo", local)
        val remoteEchoActor = client.actorFor("echo", "localhost", ctx.port)
        val finishedBarrier = new CountDownLatch(1)
        val clientActor = Actor.actorOf(new ClientReply(remoteEchoActor, finishedBarrier)).start();

        clientActor ! "Start"


        finishedBarrier.await(5, TimeUnit.SECONDS) should be(true)
      })
    }


  }

}

class ClientReply(remoteActor: ActorRef, doneBarrier: CountDownLatch) extends Actor {
  protected def receive = {
    case "Start" => {
      remoteActor ! "Start"
    }
    case "Answer for Start" => {
      self.reply("Answer")
    }
    case "Answer for Answer" => {
      doneBarrier.countDown()
    }
  }
}

class ReceiveCheckActor(barrier: Option[CountDownLatch] = null) extends Actor {
  protected def receive = {

    case "Hello-Receive-Only" => {
      barrier.get.countDown()
    }
    case x: String => {
      self.reply("Answer for " + x)
    }

  }
}

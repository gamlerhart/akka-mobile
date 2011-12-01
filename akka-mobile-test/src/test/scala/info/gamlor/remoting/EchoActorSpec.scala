package info.gamlor.remoting

import org.scalatest.WordSpec
import akka.testkit.TestKit
import akka.mobile.testutils.TestServer._
import org.scalatest.matchers.ShouldMatchers
import akka.actor.{ActorRef, Actor}
import akka.mobile.testutils.{NetworkUtils, TestDevice}
import akka.mobile.server.NettyRemoteServer
import java.util.concurrent._
import akka.mobile.client.MobileRemoteClient

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

class EchoActorSpec extends WordSpec with ShouldMatchers with TestKit {

  val client = MobileRemoteClient.createClient(TestDevice())
  "The Actor " must {

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
    "get answer " in {

      withRunningServer(ctx => {
        val local = Actor.actorOf(new ReceiveCheckActor(None)).start()
        ctx.register("echo", local)
        val echo = client.actorFor("echo", "localhost", ctx.port)
        val response = echo ? "Ask"

        response.get should be("Answer for Ask")
      })
    }
    "get answer from peer" in {

      withRunningServer(ctx => {
        val resultQueue = new ArrayBlockingQueue[String](1)
        val local = Actor.actorOf(new ServerAsksClient(resultQueue)).start()
        ctx.register("echo", local)
        val remoteEchoActor = client.actorFor("echo", "localhost", ctx.port)
        val clientActor = Actor.actorOf(new ClientReply(remoteEchoActor, new CountDownLatch(1))).start();
        clientActor ! "Start"

        resultQueue.poll(5, TimeUnit.SECONDS) should be("Answer")
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


        finishedBarrier.await(10, TimeUnit.SECONDS) should be(true)
      })
    }
    "can bind server to 0.0.0.0" in {

      val port = NetworkUtils.findFreePort()
      val server = NettyRemoteServer.start("0.0.0.0", port);
      val local = Actor.actorOf(new ReceiveCheckActor(None)).start()
      server.register("echo", local)
      val remoteEchoActor = client.actorFor("echo", "localhost", port)
      val finishedBarrier = new CountDownLatch(1)
      val clientActor = Actor.actorOf(new ClientReply(remoteEchoActor, finishedBarrier)).start();

      clientActor ! "Start"


      finishedBarrier.await(5, TimeUnit.SECONDS) should be(true)
      server.shutdownServerModule();
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

class ServerAsksClient(postQue: BlockingQueue[String]) extends Actor {
  protected def receive = {
    case "Start" => {
      val value = (self.sender.get ? "Answer for Start").get.asInstanceOf[String]
      postQue.offer(value)
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

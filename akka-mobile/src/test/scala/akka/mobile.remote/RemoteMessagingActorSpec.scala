package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.testkit.TestKit
import akka.util.Duration
import akka.config.Supervision.{Permanent, Supervise, SupervisorConfig, OneForOneStrategy}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{TimeUnit, CountDownLatch}
import java.io.IOException
import akka.actor.{ActorRef, Supervisor, Actor}

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class RemoteMessagingActorSpec extends Spec with ShouldMatchers with TestKit {

  class ThrowOnSend extends RemoteMessageChannel(new MockSocket) {
    override def send(msg: AkkaMobileProtocol) {
      throw new IOException("write failed")
    }
  }

  val msg = SendMessage(null, None);

  describe("On IOException") {
    it("it reports it back") {
      val actor = Actor.actorOf(new RemoteMessagingActor(() => new ThrowOnSend())).start()
      actor ! msg;

      val response = receiveOne(Duration.Inf)
      response match {
        case SendingFailed(x, m) => {
          m should be(msg)
          x.getMessage should be("write failed")
        }
        case _ => fail("Didn't get expected message")
      }
    }
    it("closes and kills itself") {
      var closedOldChannel = new AtomicBoolean(false);
      val channel = new ThrowOnSend() {
        override def close() {
          super.close()
          closedOldChannel.set(true);
        }
      }
      val actor = Actor.actorOf(new RemoteMessagingActor(() => {
        channel
      }))

      val killedItself = superviseAndTellIfFailed(actor)

      actor ! msg

      receiveOne(Duration.Inf)

      closedOldChannel.get() should be(true)
      killedItself.await(60, TimeUnit.SECONDS)
      actor.isShutdown should be(true)
    }

    it("while opening") {
      val actor = Actor.actorOf(new RemoteMessagingActor(() => {
        throw new IOException()
      }))

      val killedItself = superviseAndTellIfFailed(actor)
      actor ! msg
      killedItself.await(60, TimeUnit.SECONDS)
      assert(receiveOne(Duration.Inf).isInstanceOf[SendingFailed])
    }

    it("can send messaged during reopening") {

      val actor = Actor.actorOf(new RemoteMessagingActor(() => new RemoteMessageChannel(new MockSocket) {
        var callCount = 0;

        override def send(msg: AkkaMobileProtocol) {
          callCount = callCount + 1;
          if (callCount > 2) {
            throw new IOException()
          }
        }
      }))

      Supervisor(SupervisorConfig(OneForOneStrategy(List(classOf[IOException]), 10, 100),
        Supervise(actor, Permanent) :: Nil))


      actor ! msg
      assert(receiveOne(Duration.Inf).isInstanceOf[SendingSucceeded])
      actor ! msg
      assert(receiveOne(Duration.Inf).isInstanceOf[SendingSucceeded])
      actor ! msg
      assert(receiveOne(Duration.Inf).isInstanceOf[SendingFailed])

    }

  }

  def superviseAndTellIfFailed(actor: ActorRef): CountDownLatch = {
    val killedItself = new CountDownLatch(1);
    Supervisor(SupervisorConfig(OneForOneStrategy(List(classOf[IOException]), 0, 100),
      Supervise(actor, Permanent) :: Nil,
      (_, _) => {
        killedItself.countDown()
      }))
    killedItself
  }
}
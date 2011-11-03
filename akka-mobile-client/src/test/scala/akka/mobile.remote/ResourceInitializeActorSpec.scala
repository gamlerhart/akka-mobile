package akka.mobile.remote

import org.scalatest.Spec
import akka.testkit.TestKit
import org.scalatest.matchers.ShouldMatchers
import actors.threadpool.AtomicInteger
import akka.actor.{Supervisor, Actor}
import akka.config.Supervision.{Permanent, Supervise, OneForOneStrategy, SupervisorConfig}
import java.util.concurrent.{TimeUnit, CountDownLatch}
import java.io.Closeable
import java.lang.Exception

/**
 * @author roman.stoffel@gamlor.info
 * @since 29.10.11
 */

class ResourceInitializeActorSpec extends Spec with ShouldMatchers with TestKit {

  describe("The resource initializer") {
    it("returns resource") {
      val toTest = Actor.actorOf(ResourceInitializeActor(() => "theResource")).start()
      val resource = toTest.ask(ResourceInitializeActor.GetResource).get

      resource should be("theResource")

    }
    it("initializes resource once") {
      val counter = new AtomicInteger(0)
      val toTest = Actor.actorOf(ResourceInitializeActor(() => counter.incrementAndGet())).start()

      toTest.ask(ResourceInitializeActor.GetResource).get should be(1)
      toTest.ask(ResourceInitializeActor.GetResource).get should be(1)
      toTest.ask(ResourceInitializeActor.GetResource).get should be(1)
    }
    it("gets new resource after crash") {
      val counter = new AtomicInteger(0)
      val toTest = Actor.actorOf(ResourceInitializeActor(() => {
        val n = counter.incrementAndGet();
        if (1 == n) {
          throw new Exception("Oh, failed")
        }
        n
      }))

      Supervisor(
        SupervisorConfig(
          OneForOneStrategy(List(classOf[Exception]), 5, 5000),
          Supervise(toTest, Permanent) :: Nil))

      // trigger failure
      toTest.ask(ResourceInitializeActor.GetResource)

      toTest.ask(ResourceInitializeActor.GetResource).get should be(2)
      toTest.ask(ResourceInitializeActor.GetResource).get should be(2)
    }
    it("closes the resource when stopped") {
      val closedOldChannel = new CountDownLatch(1);
      val closable = new Closeable {
        def close() {
          closedOldChannel.countDown()
        }
      }

      val toTest = Actor.actorOf(ResourceInitializeActor(() => closable, (c: Closeable) => c.close())).start()

      toTest.ask(ResourceInitializeActor.GetResource).await

      toTest.stop()

      closedOldChannel.await(5, TimeUnit.SECONDS) should be(true)
    }
    it("closes the resource when restarted ") {
      val closedOldChannel = new CountDownLatch(1);
      val closable = new Closeable {
        def close() {
          closedOldChannel.countDown()
        }
      }
      val toTest = Actor.actorOf(ResourceInitializeActor(() => closable, (c: Closeable) => c.close())).start()

      toTest.ask(ResourceInitializeActor.GetResource).await

      toTest.restart(new Exception("restart"), None, None)
      closedOldChannel.await(5, TimeUnit.SECONDS) should be(true)

    }
  }

}
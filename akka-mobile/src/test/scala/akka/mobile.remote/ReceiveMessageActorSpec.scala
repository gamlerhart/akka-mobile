package akka.mobile.remote

import org.scalatest.Spec
import akka.testkit.TestKit
import java.io.IOException
import akka.mobile.protocol.MobileProtocol.MobileMessageProtocol
import akka.actor.Actor
import java.util.concurrent.{TimeUnit, CountDownLatch}
import org.scalatest.matchers.ShouldMatchers

/**
 * @author roman.stoffel@gamlor.info
 * @since 29.10.11
 */

class ReceiveMessageActorSpec extends Spec with ShouldMatchers with TestKit with TestMesssageProducer {

  class ThrowOnReceive extends RemoteMessageChannel(new MockSocket) {
    override def receive() = {
      throw new IOException();
    }
  }

  describe("Receiver Actor") {
    it("dispatches message") {
      val msg = buildMockMsg()
      val socket = new MockSocket()
      msg.writeDelimitedTo(socket.out)
      msg.writeDelimitedTo(socket.out)

      val expectMsg = new CountDownLatch(2);
      val expectMsgMock = new WireMessageDispatcher(new ActorRegistry) {
        override def dispatchToActor(message: MobileMessageProtocol) {
          expectMsg.countDown()
        }
      }

      val initializer = Actor.actorOf(ResourceInitializeActor(() => new RemoteMessageChannel(socket)));
      val mockChannel = Actor.actorOf(new ReceiveChannelMonitoring(initializer, expectMsgMock));

      initializer.start()
      mockChannel.start()

      expectMsg.await(5, TimeUnit.SECONDS) should be(true)

    }

  }

}
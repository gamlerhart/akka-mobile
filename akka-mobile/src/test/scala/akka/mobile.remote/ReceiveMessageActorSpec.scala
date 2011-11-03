package akka.mobile.remote

import org.scalatest.Spec
import akka.testkit.TestKit
import java.io.IOException
import akka.mobile.protocol.MobileProtocol.MobileMessageProtocol
import java.util.concurrent.{TimeUnit, CountDownLatch}
import org.scalatest.matchers.ShouldMatchers
import akka.actor.Actor

/**
 * @author roman.stoffel@gamlor.info
 * @since 29.10.11
 */

class ReceiveMessageActorSpec extends Spec with ShouldMatchers with TestKit with TestMesssageProducer {

  class ThrowOnReceive extends RemoteMessageChannel(new MockSocket) {
    override def receive() = {
      throw new IOException("receive failed");
    }
  }

  describe("Receiver Actor") {
    it("dispatches message") {
      val msg = buildMockMsg()
      val socket = new MockSocket()
      msg.writeDelimitedTo(socket.out)
      msg.writeDelimitedTo(socket.out)

      val expectMsg = new CountDownLatch(2);
      val expectMsgMock = new WireMessageDispatcher(new Registry, ClientSideSerialisation) {
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
    it("closes on failure") {
      val closedConnection = new CountDownLatch(1);
      val expectMsgMock = new WireMessageDispatcher(new Registry, ClientSideSerialisation)

      val initializer = Actor.actorOf(ResourceInitializeActor(() => new ThrowOnReceive() {
        override def close() {
          closedConnection.countDown()
        }
      }));
      val receiveActor = Actor.actorOf(new ReceiveChannelMonitoring(initializer, expectMsgMock));

      initializer.start()
      receiveActor.start()

      closedConnection.await(5, TimeUnit.SECONDS) should be(true)
    }
  }

}
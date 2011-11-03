package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import java.net.InetSocketAddress
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.testkit.TestKit
import akka.util.Duration
import java.io._
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{TimeUnit, CountDownLatch}

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class CanDoRemoteMessaging extends Spec with ShouldMatchers with TestMesssageProducer with TestKit {
  val MOCK_ADDRESS = new InetSocketAddress("mock.address.at.localhost", 8080);
  describe("The remote actors factor") {
    it("opens a socket only once") {
      val socket = new MockSocket()
      var callCounter = new AtomicInteger();
      val channelFactory = RemoteMessaging(a => {
        callCounter.incrementAndGet()
        socket
      })
      channelFactory.channelFor(new InetSocketAddress("localhost", 8080)) ! SendMessage(buildMockMsg(), None)
      channelFactory.channelFor(new InetSocketAddress("localhost", 8080)) ! SendMessage(buildMockMsg(), None)

      receiveOne(Duration.Inf)
      receiveOne(Duration.Inf)

      callCounter.get() should be(1)
    }
  }
  describe("The remote actor") {
    it("sends messages to the socket") {
      val outS = new PipedOutputStream()
      val inS = new PipedInputStream(outS);
      val toTest = RemoteMessaging(a => new SocketRepresentation {
        def close() {}

        def in = inS

        def out = outS
      })
      val msgChannel = toTest.channelFor(MOCK_ADDRESS)
      val msg = buildMockMsg()
      msgChannel ! SendMessage(msg, None)

      inS.synchronized {
        while (0 == inS.available()) {
          inS.wait()
        }
      }
      val msgFromSocket = AkkaMobileProtocol.parseDelimitedFrom(inS);
      msgFromSocket should not be (null)
    }
    it("stops trying to send message after certain failure amout") {
      val counter = new CountDownLatch(5)
      val channel = RemoteMessaging(a => new MockSocket() {
        override def out = {
          counter.countDown()
          throw new IOException()
        }
      }).channelFor(MOCK_ADDRESS)

      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)


      counter.await(10, TimeUnit.SECONDS) should be(true)
    }

  }
  describe("With the remote message actor") {
    it("you can get an for host and port") {
      val toTest = RemoteMessaging(a => new MockSocket)
      val msgChannel = toTest.channelFor(MOCK_ADDRESS)
      msgChannel should not be (null)
    }
    it("you can get the same actor for same host & ip") {
      val toTest = RemoteMessaging(a => new MockSocket)
      val msgC1 = toTest.channelFor(MOCK_ADDRESS)
      val msgC2 = toTest.channelFor(MOCK_ADDRESS)
      msgC1 should be(msgC2)
    }
  }

}
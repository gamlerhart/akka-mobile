package akka.mobile.client

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import java.net.InetSocketAddress
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.testkit.TestKit
import java.util.concurrent.atomic.AtomicBoolean
import java.io._
import akka.mobile.communication.CommunicationMessages._
import akka.mobile.testutils.{BlackholeActor, TestDevice}
import java.util.concurrent.{TimeoutException, CyclicBarrier, CountDownLatch, TimeUnit}

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class CanDoRemoteMessaging extends Spec with ShouldMatchers with TestMesssageProducer with TestKit {
  val MOCK_ADDRESS = new InetSocketAddress("mock.address.at.localhost", 8080);
  describe("The remote actors factory") {
    it("opens a socket only once") {
      val socket = new MockSocket()
      var callCounter = new CyclicBarrier(2);
      val channelFactory = RemoteMessaging(a => {
        callCounter.await()
        socket
      }, TestDevice.clientId, self)
      channelFactory.channelFor(new InetSocketAddress("localhost", 8080)) ! SendMessage(buildMockMsg(), None)
      channelFactory.channelFor(new InetSocketAddress("localhost", 8080)) ! SendMessage(buildMockMsg(), None)

      // should work because it's initialized
      callCounter.await(5, TimeUnit.SECONDS)

      // should throw, cause we never call the factory again
      intercept[TimeoutException] {
        callCounter.await(5, TimeUnit.SECONDS)
      }
    }
  }
  describe("The remote actor") {
    it("sends messages to the socket") {
      val outS = new PipedOutputStream()
      val inS = new PipedInputStream(outS);
      val toTest = RemoteMessaging(a => new SocketRepresentation {
        def close() {}

        def in = new ByteArrayInputStream(Array())

        def out = outS
      }, TestDevice.clientId, BlackholeActor())
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
          throw new IOException("IO Failure")
        }
      }, TestDevice.clientId, BlackholeActor()).channelFor(MOCK_ADDRESS)

      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)
      channel ! SendMessage(buildMockMsg(), None)


      counter.await(10, TimeUnit.SECONDS) should be(true)
    }
    it("It doesn't loose a message on retry") {
      val outS = new PipedOutputStream()
      val inS = new PipedInputStream(outS);
      val hasAlreadyTried = new AtomicBoolean()
      val toTest = RemoteMessaging(a => new SocketRepresentation {
        def close() {}

        def in = hasAlreadyTried.getAndSet(true) match {
          case true => new ByteArrayInputStream(Array())
          case false => throw new IOException("IO Failure")
        }

        def out = hasAlreadyTried.getAndSet(true) match {
          case true => outS
          case false => throw new IOException("IO Failure")
        }
      }, TestDevice.clientId, BlackholeActor())
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
      msgFromSocket.getMessage.getUuid.getHigh should be(msg.getMessage.getUuid.getHigh)
    }

  }
  describe("With the remote message actor") {
    it("you can get an for host and port") {
      val toTest = RemoteMessaging(a => new MockSocket, TestDevice.clientId, BlackholeActor())
      val msgChannel = toTest.channelFor(MOCK_ADDRESS)
      msgChannel should not be (null)
    }
    it("you can get the same actor for same host & ip") {
      val toTest = RemoteMessaging(a => new MockSocket, TestDevice.clientId, BlackholeActor())
      val msgC1 = toTest.channelFor(MOCK_ADDRESS)
      val msgC2 = toTest.channelFor(MOCK_ADDRESS)
      msgC1 should be(msgC2)
    }
  }

}
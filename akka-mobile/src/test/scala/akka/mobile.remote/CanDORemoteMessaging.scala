package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import java.net.InetSocketAddress
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import java.lang.ProcessImpl
import java.io.{PipedOutputStream, PipedInputStream}

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class CanDoRemoteMessaging extends Spec with ShouldMatchers with TestMesssageProducer {

  describe("The remote actors factor") {
    it("opens a socket only once") {
      val socket = new MockSocket()
      var callCounter = 0;
      val channelFactory = RemoteMessaging(a => {
        callCounter += 1
        socket
      })
      channelFactory.channelFor(new InetSocketAddress("localhost", 8080))
      channelFactory.channelFor(new InetSocketAddress("localhost", 8080))

      callCounter should be(1)
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
      val msgChannel = toTest.channelFor(new InetSocketAddress("localhost", 8080))
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

  }
  describe("With the remote message actor") {
    it("you can get an for host and port") {
      val toTest = RemoteMessaging(a => new MockSocket)
      val msgChannel = toTest.channelFor(new InetSocketAddress("localhost", 8080))
      msgChannel should not be (null)
    }
    it("you can get the same actor for same host & ip") {
      val toTest = RemoteMessaging(a => new MockSocket)
      val msgC1 = toTest.channelFor(new InetSocketAddress("localhost", 8080))
      val msgC2 = toTest.channelFor(new InetSocketAddress("localhost", 8080))
      msgC1 should be(msgC2)
    }
  }

}
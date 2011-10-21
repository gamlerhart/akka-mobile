package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import java.net.InetSocketAddress
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import com.eaio.uuid.UUID
import akka.mobile.protocol.MobileProtocol._

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

class CanRemoteMessage extends Spec with ShouldMatchers {
  val toTest = RemoteMessaging(a => new MockSocket())

  describe("Remote Messaging") {
    it("can get message channel for host and port") {
      val msgChannel = toTest.channelFor(new InetSocketAddress("localhost", 8080))
      msgChannel should not be (null)
    }
    it("writes bytes to socket") {
      val socket = new MockSocket()
      val msgChannel = RemoteMessaging(a => socket).channelFor(new InetSocketAddress("localhost", 8080))
      val msg = buildMockMsg()
      msgChannel.send(msg)

      val restoredMsg = AkkaMobileProtocol.parseDelimitedFrom(socket.asInputStreamFrom(9)).getMessage
      restoredMsg should be eq (msg)
    }
    it("opens socket once") {
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
    it("receives message") {
      val socket = new MockSocket()
      val msg = buildMockMsg();
      AkkaMobileProtocol.newBuilder().setMessage(msg).build().writeDelimitedTo(socket.out);


      val restoredMsg = RemoteMessaging(a => socket).channelFor(new InetSocketAddress("localhost", 8080)).receive();
      restoredMsg should be eq (msg)
    }
  }


  private def buildMockMsg() = {
    val newUUID = () => {
      val uuid = new UUID()
      UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode).build()
    }
    MobileMessageProtocol.newBuilder()
      .setUuid(newUUID())
      .setOneWay(true)
      .setActorInfo({
      ActorInfoProtocol.newBuilder()
        .setActorType(ActorType.SCALA_ACTOR)
        .setId("remote-actor")
        .setTarget("a.class.name")
    })
      .build()
  }

}


class MockSocket() extends SocketRepresentation {
  val outBuffer = new ByteArrayOutputStream()

  def in = new ByteArrayInputStream(outBuffer.toByteArray)

  def out = outBuffer

  def close() = outBuffer.close()

  def asInputStreamFrom(startLocation: Int) = new ByteArrayInputStream(outBuffer.toByteArray.drop(startLocation))
}
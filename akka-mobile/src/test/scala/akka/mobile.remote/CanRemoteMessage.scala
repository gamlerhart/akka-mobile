package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import java.net.{ServerSocket, InetSocketAddress}
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import com.eaio.uuid.UUID
import akka.remote.protocol.RemoteProtocol.{ActorType, ActorInfoProtocol, UuidProtocol, RemoteMessageProtocol}

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

class CanRemoteMessage extends Spec with ShouldMatchers {
  val toTest = RemoteMessaging(a=>new MockSocket())

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

      val restoredMsg = RemoteMessageProtocol.parseDelimitedFrom(socket.writtenBytesAsInputStream)
      restoredMsg should be eq (msg)
    }
  }


  private def buildMockMsg() = {
    val uuid = new UUID()
    val newUUID = () => {
      UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode).build()
    }
    RemoteMessageProtocol.newBuilder()
      .setUuid(newUUID())
      .setOneWay(true)
      .setActorInfo({
          ActorInfoProtocol.newBuilder()
            .setActorType(ActorType.SCALA_ACTOR)
            .setUuid(newUUID())
            .setId("remote-actor")
            .setTarget("a.class.name")
            .setTimeout(1000)
        })
      .build()
  }

}


class MockSocket() extends SocketRepresentation {
  val outBuffer = new ByteArrayOutputStream()

  def in = throw new Error("Not implemented")

  def out = outBuffer

  def close() = outBuffer.close()

  def writtenBytesAsInputStream = new ByteArrayInputStream(outBuffer.toByteArray)
}
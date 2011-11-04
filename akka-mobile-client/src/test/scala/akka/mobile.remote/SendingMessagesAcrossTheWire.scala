package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import com.eaio.uuid.UUID
import akka.mobile.protocol.MobileProtocol._
import akka.mobile.protocol.MobileProtocol
import akka.mobile.testutils.MockSerialisation

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

class SendingMessagesAcrossTheWire extends Spec with ShouldMatchers with TestMesssageProducer {
  describe("Remote Messaging") {
    it("writes bytes to socket") {
      val socket = new MockSocket()
      val msgChannel = channelFor(socket)
      val msg = buildMockMsg()
      msgChannel.send(msg)

      val restoredMsg = AkkaMobileProtocol.parseDelimitedFrom(socket.asInputStreamFrom(0)).getMessage

      shouldBeEqual(restoredMsg, msg.getMessage)
    }
    it("receives message") {
      val socket = new MockSocket()
      val msg = buildMockMsg();
      msg.writeDelimitedTo(socket.out);


      val restoredMsg = channelFor(socket)
        .receive();
      shouldBeEqual(restoredMsg.getMessage, msg.getMessage)
    }

  }

  private def channelFor(socket: SocketRepresentation): RemoteMessageChannel = {
    new RemoteMessageChannel(socket)
  }

  private def shouldBeEqual(restoredMsg: MobileProtocol.MobileMessageProtocol, msg: MobileProtocol.MobileMessageProtocol) {
    restoredMsg.getActorInfo.getId should be(msg.getActorInfo.getId)
    restoredMsg.getActorInfo.getActorType should be(msg.getActorInfo.getActorType)
    restoredMsg.getUuid.getHigh should be(msg.getUuid.getHigh)
    restoredMsg.getUuid.getLow should be(msg.getUuid.getLow)
  }

}

trait TestMesssageProducer {


  def buildMockMsg() = {
    val newUUID = () => {
      val uuid = new UUID()
      UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode).build()
    }
    val mobileMsg = MobileMessageProtocol.newBuilder()
      .setUuid(newUUID())
      .setNodeAddress(MockSerialisation.mockAddress())
      .setOneWay(true)
      .setActorInfo({
      ActorInfoProtocol.newBuilder()
        .setActorType(ActorType.SCALA_ACTOR)
        .setId("remote-actor")
        .setTarget("a.class.name")
    })
    mobileMsg.setSender(RemoteActorRefProtocol.newBuilder()
      .setClassOrServiceName("mock-service-id")
    )
    AkkaMobileProtocol.newBuilder().setMessage(mobileMsg).build()
  }

}



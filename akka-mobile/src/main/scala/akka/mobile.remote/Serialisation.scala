package akka.mobile.remote

import com.eaio.uuid.UUID
import akka.remote.protocol.RemoteProtocol._
import java.io.{ObjectOutputStream, ByteArrayOutputStream}
import com.google.protobuf.ByteString

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

object Serialisation {
  def toWireProtocol(rmp: RemoteMessageProtocol): AkkaRemoteProtocol = {
    val arp = AkkaRemoteProtocol.newBuilder
    arp.setMessage(rmp)
    arp.build
  }

  def oneWayMessageToActor( senderUUID:UUID, actorID:String, msg : Any) = {
    val serializeUUID = (uuid:UUID) => {
      UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode)
    }
    RemoteMessageProtocol.newBuilder()
      .setUuid(serializeUUID(new UUID()))
      .setOneWay(true)
      .setMessage(serializeMsg(msg))
      .setActorInfo({
          ActorInfoProtocol.newBuilder()
            .setActorType(ActorType.SCALA_ACTOR)
            .setUuid(serializeUUID(senderUUID))
            .setId(actorID)
            .setTarget(actorID)
            .setTimeout(1000)
        })
      .build()
  }

  private def serializeMsg(msg : Any) : MessageProtocol.Builder = {
    val msgBuilder = MessageProtocol.newBuilder();
    msgBuilder.setSerializationScheme(SerializationSchemeType.JAVA)
    msgBuilder.setMessage(ByteString.copyFrom(javaSerialize(msg)))
    msgBuilder;
  }

  private def javaSerialize(msg : Any) : Array[Byte]={
      val bos = new ByteArrayOutputStream
      val out = new ObjectOutputStream(bos)
      out.writeObject(msg)
      out.close()
      bos.toByteArray
  }
}
package akka.mobile.remote

import com.eaio.uuid.UUID
import akka.remote.protocol.RemoteProtocol._

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

  def oneWayMessageToActor( senderUUID:UUID, actorID:String) = {
    val serializeUUID = (uuid:UUID) => {
      UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode)
    }
    RemoteMessageProtocol.newBuilder()
      .setUuid(serializeUUID(new UUID()))
      .setOneWay(true)
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
}
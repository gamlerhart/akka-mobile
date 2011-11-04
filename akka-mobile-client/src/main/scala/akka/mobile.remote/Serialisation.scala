package akka.mobile.remote

import com.eaio.uuid.UUID
import akka.mobile.protocol.MobileProtocol._
import com.google.protobuf.ByteString
import akka.actor.{LocalActorRef, ActorRef}
import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */


trait Serialisation {

  def toWireProtocol(rmp: MobileMessageProtocol): AkkaMobileProtocol = {
    val arp = AkkaMobileProtocol.newBuilder
    arp.setMessage(rmp)
    arp.build
  }

  def oneWayMessageToActor(actorID: String, sender: Option[ActorRef], msg: Any) = {
    val serializeUUID = (uuid: UUID) => {
      UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode)
    }
    val builder = MobileMessageProtocol.newBuilder()
      .setUuid(serializeUUID(new UUID()))
      .setOneWay(true)
      .setMessage(serializeMsg(msg))
      .setActorInfo({
      ActorInfoProtocol.newBuilder()
        .setActorType(ActorType.SCALA_ACTOR)
        .setId(actorID)
        .setTarget(actorID)
    })

    sender match {
      case Some(r) => {
        builder.setSender(toRemoteActorRefProtocol(r))
      }
      case None => throw new Error("Todo")
    }

    builder.build()
  }

  def toAddressProtocol(actorRef: ActorRef): AddressProtocol

  def deSerializeSender(msg: MobileMessageProtocol, ctxInfo: InetSocketAddress): Option[ActorRef] = {
    if (msg.hasSender) {
      Some(deSerializeActorRef(msg.getSender, ctxInfo))
    } else {
      None
    }
  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol, ctxInfo: InetSocketAddress): ActorRef


  private def toRemoteActorRefProtocol(actor: ActorRef): RemoteActorRefProtocol = actor match {
    case localActor: LocalActorRef ⇒

      RemoteActorRefProtocol.newBuilder
        .setClassOrServiceName("uuid:" + localActor.uuid.toString)
        .setNodeAddress(toAddressProtocol(localActor))
        .build
    case _ => throw new Error("Not implemented")
  }

  private def serializeMsg(msg: Any): MessageProtocol.Builder = {
    val msgBuilder = MessageProtocol.newBuilder();
    msgBuilder.setSerializationScheme(SerializationSchemeType.JAVA)
    msgBuilder.setMessage(ByteString.copyFrom(javaSerialize(msg)))
    msgBuilder;
  }


  def deSerializeMsg(msg: MobileMessageProtocol, ctxInfo: InetSocketAddress) = {
    val deserializedMsg = msg.getMessage.getSerializationScheme match {
      case SerializationSchemeType.JAVA => javaDeSerialize(msg.getMessage.getMessage.toByteArray)
      case _ => throw new Error("Not yet implemented")
    }
    val senderOption = deSerializeSender(msg, ctxInfo)
    (deserializedMsg, senderOption)
  }

  private def javaSerialize(msg: Any): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    val out = new ObjectOutputStream(bos)
    out.writeObject(msg)
    out.close()
    bos.toByteArray
  }

  private def javaDeSerialize(bytes: Array[Byte]): AnyRef = {
    val in = new ObjectInputStream(new ByteArrayInputStream(bytes))
    try {
      in.readObject
    } finally {
      in.close()
    }
  }
}

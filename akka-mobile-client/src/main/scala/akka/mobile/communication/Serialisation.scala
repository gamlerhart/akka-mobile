package akka.mobile.communication

import com.google.protobuf.ByteString
import akka.actor.{LocalActorRef, ActorRef}
import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import java.net.InetSocketAddress
import com.eaio.uuid.UUID
import akka.mobile.protocol.MobileProtocol._
import akka.mobile.protocol.MobileProtocol

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

  def buildTarget(serviceOrId: Either[String, UUID]): RemoteActorRefProtocol.Builder = {
    val ref = RemoteActorRefProtocol.newBuilder();
    serviceOrId match {
      case Left(service) => {
        ref.setServiceName(service)
      }
      case Right(actorId) => {
        ref.setUuid(serializeUUID(actorId))
      }
    }
    ref
  }

  def messageToActor(actorID: Either[String, UUID], sender: Option[ActorRef],
                     msg: Any, replyUUID: Option[UUID]) = {

    val builder = MobileMessageProtocol.newBuilder()
      .setUuid(serializeUUID(replyUUID.getOrElse(new UUID())))
      .setNodeAddress(toAddressProtocol())
      .setMessage(serializeMsg(msg))
      .setOneWay(replyUUID.isEmpty)
      .setActorInfo({
      ActorInfoProtocol.newBuilder()
        .setActorType(ActorType.SCALA_ACTOR)
        .setTarget(buildTarget(actorID))
    })

    sender match {
      case Some(r) => {
        builder.setSender(toRemoteActorRefProtocol(r))
      }
      case None => {
        // no sender needed, yet recommended
      }
    }

    builder.build()
  }

  def response(responsID: UUID,
               result: Right[Throwable, Any]): MobileProtocol.MobileMessageProtocol = {

    val builder = MobileMessageProtocol.newBuilder()
      .setUuid(serializeUUID(new UUID()))
      .setNodeAddress(toAddressProtocol())
      .setMessage(serializeMsg(result))
      .setOneWay(true)
      .setAnswerFor(serializeUUID(responsID))
    builder.build()
  }

  def toAddressProtocol(): AddressProtocol

  def deSerializeSender(msg: MobileMessageProtocol,
                        nodeId: Option[Either[ClientId, InetSocketAddress]]): Option[ActorRef] = {
    if (msg.hasSender && nodeId.isDefined) {
      Some(deSerializeActorRef(msg.getSender, nodeId.get))
    } else {
      None
    }
  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol,
                          nodeId: Either[ClientId, InetSocketAddress]): ActorRef


  private def toRemoteActorRefProtocol(actor: ActorRef): RemoteActorRefProtocol = actor match {
    case localActor: LocalActorRef =>

      RemoteActorRefProtocol.newBuilder
        .setUuid(serializeUUID(localActor.uuid))
        .setServiceName(localActor.id.toString)
        .build
    case _ => throw new Error("Not implemented")
  }

  def serializeUUID(uuid: UUID) = {
    UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode)
  }

  def deSerializeUUID(uuid: UuidProtocol) = {
    new UUID(uuid.getHigh, uuid.getLow)
  }

  def deSerializeActorRef(actorRef: RemoteActorRefProtocol): Either[String, UUID] = {
    if (actorRef.hasUuid) {
      Right(deSerializeUUID(actorRef.getUuid))
    } else {
      Left(actorRef.getServiceName)
    }
  }


  def deSerializeMessageContent(msg: MobileProtocol.MobileMessageProtocol): AnyRef = {
    msg.getMessage.getSerializationScheme match {
      case SerializationSchemeType.JAVA => javaDeSerialize(msg.getMessage.getMessage.toByteArray)
      case _ => throw new Error("Not yet implemented")
    }
  }

  def deSerializeMsg(msg: MobileMessageProtocol, nodeId: Option[Either[ClientId, InetSocketAddress]]) = {
    val deserializedMsg: AnyRef = deSerializeMessageContent(msg)
    val senderOption = deSerializeSender(msg, nodeId)
    (deserializedMsg, senderOption)
  }

  private def serializeMsg(msg: Any): MessageProtocol.Builder = {
    val msgBuilder = MessageProtocol.newBuilder();
    msgBuilder.setSerializationScheme(SerializationSchemeType.JAVA)
    msgBuilder.setMessage(ByteString.copyFrom(javaSerialize(msg)))
    msgBuilder;
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

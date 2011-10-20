package akka.mobile.remote

import com.eaio.uuid.UUID
import akka.mobile.protocol.MobileProtocol._
import com.google.protobuf.ByteString
import akka.actor.{Actor, LocalActorRef, ActorRef}
import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */


object Serialisation {
  def toWireProtocol(rmp: MobileMessageProtocol): AkkaMobileProtocol = {
    val arp = AkkaMobileProtocol.newBuilder
    arp.setMessage(rmp)
    arp.build
  }

  def oneWayMessageToActor( senderUUID:UUID, actorID:String, sender : Option[ActorRef],  msg : Any) = {
    val serializeUUID = (uuid:UUID) => {
      UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode)
    }
    val builder = MobileMessageProtocol.newBuilder()
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

    sender match {
      case Some(r) => {
        builder.setSender(toRemoteActorRefProtocol(r))
      }
      case None => throw new Error("Todo")
    }

    builder.build()
  }


  private def toRemoteActorRefProtocol(actor: ActorRef): RemoteActorRefProtocol = actor match {
    case localActor: LocalActorRef ⇒

      RemoteActorRefProtocol.newBuilder
        .setClassOrServiceName("uuid:" + localActor.uuid.toString)
        .setActorClassname(localActor.actorClassName)
        .setHomeAddress(toAddressProtocol(localActor))
        .setTimeout(localActor.timeout)
        .build
    case _ => throw new Error("Not implemented")
  }

  private def toAddressProtocol(actorRef: ActorRef) = {
    AddressProtocol.newBuilder
      .setType(AddressType.DEVICE_ADDRESS)
      .setDeviceAddress(DeviceAddress.newBuilder().setAppId("mock").setDeviceID("mock"))
      .build
  }

  private def serializeMsg(msg : Any) : MessageProtocol.Builder = {
    val msgBuilder = MessageProtocol.newBuilder();
    msgBuilder.setSerializationScheme(SerializationSchemeType.JAVA)
    msgBuilder.setMessage(ByteString.copyFrom(javaSerialize(msg)))
    msgBuilder;
  }


  def deSerializeMsg(msg : MessageProtocol) : AnyRef= {
    msg.getSerializationScheme match {
      case SerializationSchemeType.JAVA => javaDeSerialize(msg.getMessage.toByteArray)
      case _ => throw new Error("Not yet implemented")
    }
  }

  private def javaSerialize(msg : Any) : Array[Byte]={
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

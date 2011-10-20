package akka.mobile.remote

import com.eaio.uuid.UUID
import akka.remote.protocol.RemoteProtocol._
import java.io.{ObjectOutputStream, ByteArrayOutputStream}
import com.google.protobuf.ByteString
import akka.actor.{Actor, LocalActorRef, ActorRef}
import java.net.InetSocketAddress

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

  def oneWayMessageToActor( senderUUID:UUID, actorID:String, sender : Option[ActorRef],  msg : Any) = {
    val serializeUUID = (uuid:UUID) => {
      UuidProtocol.newBuilder().setHigh(uuid.getTime).setLow(uuid.getClockSeqAndNode)
    }
    val builder = RemoteMessageProtocol.newBuilder()
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
    val address = actorRef.homeAddress.getOrElse(new InetSocketAddress("localhost",8080))
    AddressProtocol.newBuilder
      .setHostname(address.getAddress.getHostAddress)
      .setPort(address.getPort)
      .build
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
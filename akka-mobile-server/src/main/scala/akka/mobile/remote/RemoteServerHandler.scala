package akka.mobile.remote

import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.channel.{MessageEvent, ChannelHandlerContext, SimpleChannelUpstreamHandler, ChannelHandler}
import akka.mobile.protocol.MobileProtocol.ActorType._
import akka.actor.{ActorRef, IllegalActorStateException}
import akka.mobile.protocol.MobileProtocol.{AddressType, RemoteActorRefProtocol, MobileMessageProtocol, AkkaMobileProtocol}

/**
 *
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

@ChannelHandler.Sharable
class RemoteServerHandler(channels: ChannelGroup, actorRegistry: ActorRegistry)
  extends SimpleChannelUpstreamHandler with RemoteMessaging {


  override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent) {
    event.getMessage match {
      case remoteProtocol: AkkaMobileProtocol if remoteProtocol.hasMessage => {
        dispatchMessage(remoteProtocol.getMessage)
      }
      case _ => {
        throw new Error("Not implemented")
      }
    }
  }


  def send(clientId: ClientId, serviceId: String, value: Any, option: Option[ActorRef]) {

  }

  private def dispatchMessage(message: MobileMessageProtocol) {
    message.getActorInfo.getActorType match {
      case SCALA_ACTOR ⇒ dispatchToActor(message)
      case TYPED_ACTOR ⇒ throw new IllegalActorStateException("ActorType TYPED_ACTOR is currently not supported")
      case JAVA_ACTOR ⇒ throw new IllegalActorStateException("ActorType JAVA_ACTOR is currently not supported")
      case other ⇒ throw new IllegalActorStateException("Unknown ActorType [" + other + "]")
    }
  }

  private def dispatchToActor(message: MobileMessageProtocol) {
    val actorInfo = message.getActorInfo
    val actor = actorRegistry.findActorById(actorInfo.getId)


    val msgForActor = Serialisation.deSerializeMsg(message.getMessage);
    val sender = if (message.hasSender) {
      Some(deSerializeActorRef(message.getSender))
    } else {
      None
    }

    if (message.getOneWay) {
      actor.postMessageToMailbox(msgForActor, sender)
    } else {
      throw new Error("Not yet implemented")
    }
  }


  private def deSerializeActorRef(refInfo: RemoteActorRefProtocol): ActorRef = {
    val remoteActorId = refInfo.getClassOrServiceName
    val homeAddress = refInfo.getHomeAddress
    homeAddress.getType match {
      case AddressType.DEVICE_ADDRESS => {
        RemoteDeviceActorRef(Serialisation.deserializeClientId(homeAddress.getDeviceAddress),
          remoteActorId, this)
      }
      case AddressType.SERVICE_ADDRESS => throw new Error("TODO")
      case ue => throw new Error("Unexpected type " + ue)
    }
  }

}
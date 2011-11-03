package akka.mobile.remote

import org.jboss.netty.channel.group.ChannelGroup
import akka.mobile.protocol.MobileProtocol.ActorType._
import akka.mobile.protocol.MobileProtocol.{AddressType, RemoteActorRefProtocol, MobileMessageProtocol, AkkaMobileProtocol}
import java.util.concurrent.ConcurrentHashMap
import akka.actor.{ActorRef, IllegalActorStateException}
import org.jboss.netty.channel.{ChannelStateEvent, MessageEvent, ChannelHandlerContext, SimpleChannelUpstreamHandler, ChannelHandler, Channel => NettyChannel}

/**
 *
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

@ChannelHandler.Sharable
class RemoteServerHandler(channels: ChannelGroup, wireMessageDispatcher: WireMessageDispatcher)
  extends SimpleChannelUpstreamHandler with MessageSink {
  private val clientChannels = new ConcurrentHashMap[ClientId, NettyChannel]()

  override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent) {
    event.getMessage match {
      case remoteProtocol: AkkaMobileProtocol if remoteProtocol.hasMessage => {
        dispatchMessage(remoteProtocol.getMessage, ctx.getChannel)
      }
      case _ => {
        throw new Error("Not implemented")
      }
    }
  }

  override def channelOpen(ctx: ChannelHandlerContext, event: ChannelStateEvent) = channels.add(ctx.getChannel)


  def send(clientId: ClientId, serviceId: String, message: Any, sender: Option[ActorRef]) {
    val backChannel = clientChannels.get(clientId)

    val msg = AkkaMobileProtocol.newBuilder()
      .setMessage(Serialisation.oneWayMessageToActor(serviceId, sender, message))
      .build();
    backChannel.write(msg);

  }

  private def dispatchMessage(message: MobileMessageProtocol, channel: NettyChannel) {

    val sender = if (message.hasSender) {
      Some(deSerializeActorRef(message.getSender))
    } else {
      None
    }

    sender.foreach(si => {
      clientChannels.put(si.clientId, channel)
    })

    message.getActorInfo.getActorType match {
      case SCALA_ACTOR ⇒ wireMessageDispatcher.dispatchToActor(message, sender)
      case TYPED_ACTOR ⇒ throw new IllegalActorStateException("ActorType TYPED_ACTOR is currently not supported")
      case JAVA_ACTOR ⇒ throw new IllegalActorStateException("ActorType JAVA_ACTOR is currently not supported")
      case other ⇒ throw new IllegalActorStateException("Unknown ActorType [" + other + "]")
    }
  }


  def deSerializeActorRef(refInfo: RemoteActorRefProtocol): RemoteDeviceActorRef = {
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
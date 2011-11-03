package akka.mobile.remote

import org.jboss.netty.channel.group.ChannelGroup
import akka.mobile.protocol.MobileProtocol.ActorType._
import akka.mobile.protocol.MobileProtocol.{MobileMessageProtocol, AkkaMobileProtocol}
import java.util.concurrent.ConcurrentHashMap
import akka.actor.{ActorRef, IllegalActorStateException}
import org.jboss.netty.channel.{ChannelStateEvent, MessageEvent, ChannelHandlerContext, SimpleChannelUpstreamHandler, ChannelHandler, Channel => NettyChannel}

/**
 *
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

@ChannelHandler.Sharable
class RemoteServerHandler(channels: ChannelGroup, registry: Registry)
  extends SimpleChannelUpstreamHandler with MessageSink {
  private val clientChannels = new ConcurrentHashMap[ClientId, NettyChannel]()
  private val serializer = new ServerSideSerialisation(this)
  private val dispatcher = new WireMessageDispatcher(registry, new ServerSideSerialisation(this))

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

    val msg = serializer.toWireProtocol(
      serializer.oneWayMessageToActor(serviceId, sender, message))
    backChannel.write(msg);

  }

  private def dispatchMessage(message: MobileMessageProtocol, channel: NettyChannel) {

    val sender = if (message.hasSender) {
      Some(serializer.deSerializeActorRef(message.getSender))
    } else {
      None
    }

    sender.foreach(si => {
      clientChannels.put(si.asInstanceOf[RemoteDeviceActorRef].clientId, channel)
    })

    message.getActorInfo.getActorType match {
      case SCALA_ACTOR ⇒ dispatcher.dispatchToActor(message)
      case TYPED_ACTOR ⇒ throw new IllegalActorStateException("ActorType TYPED_ACTOR is currently not supported")
      case JAVA_ACTOR ⇒ throw new IllegalActorStateException("ActorType JAVA_ACTOR is currently not supported")
      case other ⇒ throw new IllegalActorStateException("Unknown ActorType [" + other + "]")
    }
  }


}
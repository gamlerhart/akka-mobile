package akka.mobile.remote

import org.jboss.netty.channel.group.ChannelGroup
import akka.mobile.protocol.MobileProtocol.ActorType._
import akka.mobile.protocol.MobileProtocol.{MobileMessageProtocol, AkkaMobileProtocol}
import java.util.concurrent.ConcurrentHashMap
import akka.actor.{ActorRef, IllegalActorStateException}
import java.net.InetSocketAddress
import org.jboss.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelStateEvent, MessageEvent, ChannelHandlerContext, SimpleChannelUpstreamHandler, ChannelHandler, Channel => NettyChannel}
import com.eaio.uuid.UUID

/**
 *
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

@ChannelHandler.Sharable
class RemoteServerHandler(channels: ChannelGroup, registry: Registry, serverInfo: ServerInfo)
  extends SimpleChannelUpstreamHandler with MessageSink {
  private val clientChannels = new ConcurrentHashMap[ClientId, NettyChannel]()
  private val serializer = new ServerSideSerialisation(this, serverInfo)
  private val futures = new FutureResultHandling
  private val dispatcher = new WireMessageDispatcher(registry, futures, this, new ServerSideSerialisation(this, serverInfo))

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


  def sendResponse(clientId: Either[ClientId, InetSocketAddress],
                   responseFor: UUID, result: Right[Throwable, Any]) {

    val backChannel = clientChannels.get(clientId.left.get)


    val msg = serializer.toWireProtocol(
      serializer.response(responseFor, result))
    backChannel.write(msg);
  }

  def send(clientId: Either[ClientId, InetSocketAddress], serviceId: String,
           message: Any, sender: Option[ActorRef], replyUUID: Option[UUID]) {
    val backChannel = clientChannels.get(clientId.left.get)

    sender.foreach(si => {
      registry.registerActor("uuid:" + si.uuid.toString, si)
    })

    val msg = serializer.toWireProtocol(
      serializer.messageToActor(serviceId, sender, message, replyUUID))
    backChannel.write(msg);

  }

  private def dispatchMessage(message: MobileMessageProtocol, channel: NettyChannel) {

    val ctxInfo = channel.getRemoteAddress.asInstanceOf[InetSocketAddress]
    val senderInfo = if (message.hasSender) {
      serializer.deSerializeActorRef(message.getSender, Right(ctxInfo))
    } else {
      throw new Error("Server cannot deal without client info")
    }

    val clientId = senderInfo.asInstanceOf[RemoteDeviceActorRef].clientId;
    val oldChannel = clientChannels.put(clientId, channel)
    if (oldChannel != channel) {
      channel.getCloseFuture.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          clientChannels.remove(clientId)
        }
      })
      if (null != oldChannel && oldChannel.isConnected) {
        oldChannel.disconnect()
      }
    }

    message.getActorInfo.getActorType match {
      case SCALA_ACTOR ⇒ dispatcher.dispatchMessage(message, Left(clientId))
      case TYPED_ACTOR ⇒ throw new IllegalActorStateException("ActorType TYPED_ACTOR is currently not supported")
      case JAVA_ACTOR ⇒ throw new IllegalActorStateException("ActorType JAVA_ACTOR is currently not supported")
      case other ⇒ throw new IllegalActorStateException("Unknown ActorType [" + other + "]")
    }
  }


}
package akka.mobile.remote

import org.jboss.netty.channel.group.ChannelGroup
import akka.mobile.protocol.MobileProtocol.ActorType._
import java.util.concurrent.ConcurrentHashMap
import akka.actor.{ActorRef, IllegalActorStateException}
import java.net.InetSocketAddress
import org.jboss.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelStateEvent, MessageEvent, ChannelHandlerContext, SimpleChannelUpstreamHandler, ChannelHandler, Channel => NettyChannel}
import com.eaio.uuid.UUID
import akka.dispatch.CompletableFuture
import akka.mobile.protocol.MobileProtocol.{AddressType, MobileMessageProtocol, AkkaMobileProtocol}

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
  private val futures = new FutureResultHandling
  private val dispatcher = new WireMessageDispatcher(registry, futures, this, new ServerSideSerialisation(this))

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


  def registerFuture(uuid: UUID, future: CompletableFuture[Any]) {
    futures.put(uuid, future)
  }

  private def dispatchMessage(message: MobileMessageProtocol, channel: NettyChannel) {

    if (message.getNodeAddress.getType == AddressType.SERVICE_ADDRESS) {
      throw new IllegalArgumentException("Cannot deal with service addresses")
    }

    val clientId = serializer.deserializeClientId(message.getNodeAddress.getDeviceAddress);
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
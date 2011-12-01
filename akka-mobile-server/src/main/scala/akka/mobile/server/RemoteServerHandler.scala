package akka.mobile.server

import org.jboss.netty.channel.group.ChannelGroup
import akka.mobile.protocol.MobileProtocol.ActorType._
import akka.mobile.protocol.MobileProtocol.{AddressType, MobileMessageProtocol, AkkaMobileProtocol}
import akka.mobile.communication._
import akka.event.EventHandler
import org.jboss.netty.channel.{ExceptionEvent, ChannelFuture, ChannelFutureListener, ChannelStateEvent, MessageEvent, ChannelHandlerContext, SimpleChannelUpstreamHandler, ChannelHandler, Channel => NettyChannel}
import akka.actor.IllegalActorStateException
import akka.mobile.communication.NetworkFailures.StartedConnecting
import akka.mobile.server.ServerEvents.{ClientDisconnected, ClientConnected}


/**
 *
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */


@ChannelHandler.Sharable
class RemoteServerHandler(channels: ChannelGroup, serverMsgHandler: ServerMessagesToClientHandler,
                          eventsReceiver: Any => Unit = ignoreEvents => {})
  extends SimpleChannelUpstreamHandler {

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

  override def channelOpen(ctx: ChannelHandlerContext, event: ChannelStateEvent) {
    channels.add(ctx.getChannel)
  }


  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    ctx.getChannel.close()
    EventHandler.notify(NettyServerError(e.getCause, "Unhandled exception in the server"))
  }

  /**
   * We want to test faulty channels. However its hard to make a real channel faulty or simulate netty.
   * So for may testing purposes it just easier to inject a 'faulty' channel.
   */
  private[server] def putChannelForClient(client: ClientId, channel: NettyChannel) = serverMsgHandler.putChannelForClient(client, channel)

  private def dispatchMessage(message: MobileMessageProtocol, channel: NettyChannel) {

    if (message.getNodeAddress.getType == AddressType.SERVICE_ADDRESS) {
      throw new IllegalArgumentException("Cannot deal with service addresses")
    }
    val clientId = serverMsgHandler.serializer.deserializeClientId(message.getNodeAddress.getDeviceAddress);
    val oldChannel = putChannelForClient(clientId, channel)
    if (oldChannel != channel) {
      channel.getCloseFuture.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          serverMsgHandler.removeChannelForClient(clientId)
          eventsReceiver(ClientDisconnected(clientId))
        }
      })
      if (null != oldChannel && oldChannel.isConnected) {
        oldChannel.disconnect()
      }
      serverMsgHandler.errorHandler ! StartedConnecting(serverMsgHandler.connectionManagerFor(clientId))
      if (null == oldChannel) {
        eventsReceiver(ClientConnected(clientId))
      }
    }
    message.getActorInfo.getActorType match {
      case SCALA_ACTOR ⇒ serverMsgHandler.dispatcher.dispatchMessage(message, Left(clientId))
      case TYPED_ACTOR ⇒ throw new IllegalActorStateException("ActorType TYPED_ACTOR is currently not supported")
      case JAVA_ACTOR ⇒ throw new IllegalActorStateException("ActorType JAVA_ACTOR is currently not supported")
      case other ⇒ throw new IllegalActorStateException("Unknown ActorType [" + other + "]")
    }
  }


}
package akka.mobile.remote

import akka.actor.ActorRef
import java.lang.IllegalStateException
import org.jboss.netty.bootstrap.ServerBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.group.ChannelGroup
import akka.remote.netty.DefaultDisposableChannelGroup
import akka.remote.RemoteServerSettings
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.codec.frame.{LengthFieldPrepender, LengthFieldBasedFrameDecoder}
import akka.remote.protocol.RemoteProtocol.AkkaRemoteProtocol
import org.jboss.netty.handler.codec.protobuf.{ProtobufEncoder, ProtobufDecoder}
import org.jboss.netty.handler.execution.{OrderedMemoryAwareThreadPoolExecutor, ExecutionHandler}
import java.util.concurrent.{TimeUnit, ThreadFactory, Executors}
import org.jboss.netty.channel._
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait MobileRemoteServer {
  def register(idOfActor: String, actorRef: ActorRef)

  def shutdownServerModule()
}



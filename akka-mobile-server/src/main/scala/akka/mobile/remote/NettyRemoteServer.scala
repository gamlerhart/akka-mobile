package akka.mobile.remote

import java.lang.IllegalStateException
import org.jboss.netty.bootstrap.ServerBootstrap
import java.net.InetSocketAddress
import akka.remote.netty.DefaultDisposableChannelGroup
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.execution.{OrderedMemoryAwareThreadPoolExecutor, ExecutionHandler}
import java.util.concurrent.{TimeUnit, Executors}
import org.jboss.netty.channel._
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.actor.ActorRef
import group.ChannelGroup
import org.jboss.netty.handler.codec.protobuf.{ProtobufVarint32LengthFieldPrepender, ProtobufVarint32FrameDecoder, ProtobufEncoder, ProtobufDecoder}

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */
object NettyRemoteServer {
  def start(hostName: String, portNumber: Int): RemoteServer
  = new NettyServer(hostName, portNumber)


  class NettyServer(hostName: String, portNumber: Int) extends RemoteServer {
    private val actorRegistry = new Registry();
    @volatile var isAlive = true
    val name = "NettyRemoteServer@" + hostName + ":" + portNumber

    private val bootstrap = new ServerBootstrap(
      new NioServerSocketChannelFactory(Executors.newCachedThreadPool, Executors.newCachedThreadPool))
    private val openChannels: ChannelGroup = new DefaultDisposableChannelGroup("akka--mobile-server")
    bootstrap.setPipelineFactory(new MobileServerPipelineFactory(openChannels, actorRegistry, new ServerInfo(hostName, portNumber)))
    bootstrap.setOption("backlog", 1024)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)


    openChannels.add(bootstrap.bind(new InetSocketAddress(hostName, portNumber)))


    def register(idOfActor: String, actorRef: ActorRef) {
      checkAlive()
      actorRegistry.registerActor(idOfActor, actorRef)
    }

    def shutdownServerModule() {
      isAlive = false;
      openChannels.disconnect()
      openChannels.close()
      bootstrap.releaseExternalResources()
    }


    private def checkAlive() {
      if (!isAlive) {
        throw new IllegalStateException("Server was shut down. Start it with NettyRemoteServer.start")
      }
    }


  }

  class MobileServerPipelineFactory(channels: ChannelGroup, actorRegistry: Registry, serverInfo: ServerInfo) extends ChannelPipelineFactory {
    def getPipeline = {
      val lenDec = new ProtobufVarint32FrameDecoder()
      val lenPrep = new ProtobufVarint32LengthFieldPrepender()
      val protobufDec = new ProtobufDecoder(AkkaMobileProtocol.getDefaultInstance)
      val protobufEnc = new ProtobufEncoder


      val executor = new ExecutionHandler(
        new OrderedMemoryAwareThreadPoolExecutor(
          16,
          0,
          0,
          60, TimeUnit.SECONDS));

      val serverHandler = new RemoteServerHandler(channels, actorRegistry, serverInfo)
      val stages: List[ChannelHandler]
      = lenDec :: protobufDec :: lenPrep :: protobufEnc :: executor :: serverHandler :: Nil
      new StaticChannelPipeline(stages: _*)
    }
  }


}
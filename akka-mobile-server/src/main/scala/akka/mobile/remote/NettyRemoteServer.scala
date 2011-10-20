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

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait MobileRemoteServer {
  def register(idOfActor: String, actorRef: ActorRef)

  def shutdownServerModule()
}


object NettyRemoteServer {
  def start(hostName: String, portNumber: Int): MobileRemoteServer
    = new NettyRemoteServer(hostName, portNumber)


  class NettyRemoteServer(hostName: String, portNumber: Int) extends MobileRemoteServer {
    @volatile var isAlive = true
    val name = "NettyRemoteServer@" + hostName + ":" + portNumber

    private val threadPool = newNamedPool(name)
    private val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(threadPool, threadPool))
    private val openChannels: ChannelGroup = new DefaultDisposableChannelGroup("akka--mobile-server")
    bootstrap.setPipelineFactory(new MobileServerPipelineFactory(openChannels))
    bootstrap.setOption("backlog", RemoteServerSettings.BACKLOG)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)


    openChannels.add(bootstrap.bind(new InetSocketAddress(hostName, portNumber)))


    def register(idOfActor: String, actorRef: ActorRef) {
      checkAlive()
    }

    def shutdownServerModule() {
      isAlive = false;
      openChannels.close()
      bootstrap.releaseExternalResources()
    }


    private def checkAlive() {
      if (!isAlive) {
        throw new IllegalStateException("Server was shut down. Start it with NettyRemoteServer.start")
      }
    }

    private def newNamedPool(name: String) =
      Executors.newCachedThreadPool(new ThreadFactory {
        def newThread(r: Runnable) = {
          val thread = Executors.defaultThreadFactory().newThread(r);
          thread.setName(thread.getName + " for " + name)
          thread.setDaemon(true)
          thread
        }
      })

  }

  class MobileServerPipelineFactory(channels : ChannelGroup) extends ChannelPipelineFactory {
    def getPipeline = {
      val lenDec = new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4)
      val lenPrep = new LengthFieldPrepender(4)
      val protobufDec = new ProtobufDecoder(AkkaRemoteProtocol.getDefaultInstance)
      val protobufEnc = new ProtobufEncoder


      val executor = new ExecutionHandler(
        new OrderedMemoryAwareThreadPoolExecutor(
          16,
          0,
          0,
          60, TimeUnit.SECONDS));

      val serverHandler = new RemoteServerHandler(channels)
      val stages: List[ChannelHandler]
          = lenDec :: protobufDec :: lenPrep :: protobufEnc :: executor :: serverHandler ::  Nil
      new StaticChannelPipeline(stages:_*)
    }
  }

  @ChannelHandler.Sharable
  class RemoteServerHandler(channels : ChannelGroup) extends SimpleChannelUpstreamHandler {

    override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent){
      event.getMessage match{
        case remoteProtocol: AkkaRemoteProtocol =>{
          println(channels)
          println("Sweeet")
        }
        case _ =>{
          throw new Error("Not implemented")
        }
      }
    }

  }

}
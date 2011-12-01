package akka.mobile.server

import org.jboss.netty.bootstrap.ServerBootstrap
import java.net.InetSocketAddress
import akka.remote.netty.DefaultDisposableChannelGroup
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.execution.{OrderedMemoryAwareThreadPoolExecutor, ExecutionHandler}
import java.util.concurrent.Executors
import org.jboss.netty.channel._
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import group.ChannelGroup
import org.jboss.netty.handler.codec.protobuf.{ProtobufVarint32LengthFieldPrepender, ProtobufVarint32FrameDecoder, ProtobufEncoder, ProtobufDecoder}
import akka.actor.{Actor, ActorRef}
import akka.mobile.communication._
import akka.mobile.server.ServerEvents._
import akka.mobile.communication.RemoteMessages.{RegisteringDone, RegisterMeForC2MD}
import java.lang.{IllegalStateException, String}

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */
object NettyRemoteServer {
  val AllIPs = "0.0.0.0"

  def start(hostName: String = AllIPs, portNumber: Int,
            errorHandler: ActorRef = DefaultServerErrorHandler(),
            database: Option[ClientInfoDatabase] = None,
            configuration: ServerConfiguration = ServerConfiguration.defaultConfig): RemoteServer
  = new NettyServer(hostName, portNumber, configuration, errorHandler, database)

  def start(): RemoteServer = start(DefaultServerErrorHandler())

  def start(errorHandler: ActorRef): RemoteServer
  = {
    val cfg = ServerConfiguration.defaultConfig
    if (cfg.HOST.isEmpty || cfg.PORT.isEmpty) {
      throw new IllegalStateException("In order to use this method you need to configure the port and host" +
        " of the server in the akka configuration")
    }
    new NettyServer(cfg.HOST.get,
      cfg.PORT.get, cfg, errorHandler, None)
  }


  class NettyServer(hostName: String, portNumber: Int,
                    config: ServerConfiguration,
                    errorHandler: ActorRef,
                    database: Option[ClientInfoDatabase]) extends RemoteServer {
    private val actorRegistry = new Registry();
    @volatile var isAlive = true
    val name = "NettyRemoteServer@" + hostName + ":" + portNumber
    private val msgSink = new ServerMessagesToClientHandler(actorRegistry, errorHandler, createC2MD(errorHandler))

    private val bootstrap = new ServerBootstrap(
      new NioServerSocketChannelFactory(Executors.newCachedThreadPool, Executors.newCachedThreadPool))
    private val openChannels: ChannelGroup = new DefaultDisposableChannelGroup("akka--mobile-server")

    bootstrap.setPipelineFactory(BuildOnceFactory)
    bootstrap.setOption("backlog", config.BACKLOG)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)


    openChannels.add(bootstrap.bind(new InetSocketAddress(hostName, portNumber)))

    register(InternalActors.ForceConnectActorName, Actor.actorOf[ForceConnectActor])
    if (config.C2MD_APP_KEY.isDefined) {
      register(InternalActors.C2MDRegistration, Actor.actorOf(C2MDRegistration))
    }

    def register(idOfActor: String, actorRef: ActorRef) {
      checkAlive()
      actorRef.start()
      actorRegistry.registerActor(idOfActor, actorRef)
    }

    def shutdownServerModule() {
      isAlive = false;
      openChannels.disconnect()
      openChannels.close()
      bootstrap.releaseExternalResources()
    }


    def actorOf(clientId: ClientId, id: String) = {
      RemoteDeviceActorRef(clientId, Left(id), msgSink)
    }


    def connectionStateOf(clientId: ClientId) = msgSink.connectionStateOf(clientId)

    private def checkAlive() {
      if (!isAlive) {
        throw new IllegalStateException("Server was shut down. Start it with NettyRemoteServer.start")
      }
    }


    private def createC2MD(errorHandler: ActorRef): FallBackPushMessageSender = {
      if (config.C2MD_APP_KEY.isDefined) {
        val db = database.getOrElse(throw new IllegalStateException("A database is required for supporing C2MD"))
        new C2MDSender(config, db, errorHandler)
      } else {
        NoC2MDAvailable
      }
    }

    object C2MDRegistration extends Actor {
      protected def receive = {
        case RegisterMeForC2MD(clientId, googleKey) => {
          notifyListeners(NewC2MDRegistration(clientId, googleKey))
          self.reply(RegisteringDone(clientId, googleKey))
          database.foreach(d => {
            d.storeKeyFor(clientId, googleKey)
          })
        }
      }
    }

    object BuildOnceFactory extends ChannelPipelineFactory {
      def getPipeline = {
        val lenDec = new ProtobufVarint32FrameDecoder()
        val lenPrep = new ProtobufVarint32LengthFieldPrepender()
        val protobufDec = new ProtobufDecoder(AkkaMobileProtocol.getDefaultInstance)
        val protobufEnc = new ProtobufEncoder


        val executor = new ExecutionHandler(
          new OrderedMemoryAwareThreadPoolExecutor(
            config.EXECUTION_POOL_SIZE,
            config.MAX_CHANNEL_MEMORY_SIZE,
            config.MAX_TOTAL_MEMORY_SIZE,
            config.EXECUTION_POOL_KEEPALIVE.length, config.EXECUTION_POOL_KEEPALIVE.unit));

        val serverHandler = new RemoteServerHandler(openChannels, msgSink, e => notifyListeners(e))
        val stages: List[ChannelHandler]
        = lenDec :: protobufDec :: lenPrep :: protobufEnc :: executor :: serverHandler :: Nil
        new StaticChannelPipeline(stages: _*)

      }
    }

  }


  class ForceConnectActor extends Actor {
    protected def receive = {
      case InternalActors.ConnectNow => {}
    }
  }


}
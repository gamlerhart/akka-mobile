package akka.mobile.server

import org.jboss.netty.bootstrap.ClientBootstrap
import java.util.concurrent.Executors
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.handler.ssl.SslHandler
import javax.net.ssl.SSLContext
import org.jboss.netty.channel._
import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.mobile.communication.CommunicationMessages.SendMessage
import akka.mobile.communication.NetworkFailures.{ConnectionError, CannotSendDueNoConnection}
import org.jboss.netty.handler.codec.http._
import akka.event.EventHandler
import java.lang.IllegalStateException
import java.net.{URLEncoder, URI, InetSocketAddress}
import akka.mobile.communication.ClientId
import akka.mobile.server.C2MDFailures.{NoC2MDRegistrationForDevice, C2MDServerError, CouldNotConnectToC2MD}

/**
 * @author roman.stoffel@gamlor.info
 * @since 26.11.11
 */

trait FallBackPushMessageSender {
  def canSendC2MDToThisClient(clientId: ClientId): Boolean

  def sendRequest(deviceId: ClientId, message: AkkaMobileProtocol, sender: Option[ActorRef], connectionHandlingActor: ActorRef)
}

object NoC2MDAvailable extends FallBackPushMessageSender {
  def sendRequest(deviceId: ClientId, message: AkkaMobileProtocol, sender: Option[ActorRef], connectionHandlingActor: ActorRef) {
    throw new IllegalStateException("No C2MD service is set up")
  }

  def canSendC2MDToThisClient(clientId: ClientId) = false
}

class C2MDSender(config: ServerConfiguration,
                 database: ClientInfoDatabase,
                 errorHandler: ActorRef) extends FallBackPushMessageSender {
  val bootstrap = new ClientBootstrap(
    new NioClientSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()));
  bootstrap.setPipelineFactory(new HttpClientPipelineFactory());

  val authToken = config.C2MD_APP_KEY.getOrElse(throw new IllegalStateException("Need a c2md app key to use the google service"))
  val uriString = config.C2MD_URL
  val uri = new URI(config.C2MD_URL)
  val host = uri.getHost
  val port = uri.getPort match {
    case -1 => 443
    case p => p
  }

  def sendRequest(deviceId: ClientId, message: AkkaMobileProtocol,
                  sender: Option[ActorRef], connectionHandlingActor: ActorRef) {
    database.apiKeyFor(deviceId) match {
      case None => {
        errorHandler ! NoC2MDRegistrationForDevice(deviceId,
          SendMessage(message, sender), connectionHandlingActor)

      }
      case Some(googleDeviceKey) => {
        sendRequestToDevice(googleDeviceKey, message, sender, connectionHandlingActor)
      }

    }

  }


  def canSendC2MDToThisClient(clientId: ClientId) = {
    database.apiKeyFor(clientId) match {
      case Some(_) => true
      case None => false
    }
  }

  def sendRequestToDevice(googleDeviceKey: String, message: AkkaMobileProtocol, sender: Option[ActorRef], connectionHandlingActor: ActorRef) {
    val future = bootstrap.connect(new InetSocketAddress(host, port));

    future.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (future.isSuccess) {
          writeMessageToChannel(future.getChannel, googleDeviceKey, message, sender: Option[ActorRef], connectionHandlingActor)
        } else {
          errorHandler ! CouldNotConnectToC2MD(future.getCause,
            SendMessage(message, sender), connectionHandlingActor)
          future.getChannel.disconnect()
          future.getChannel.close()
        }
      }
    })
  }

  def writeMessageToChannel(channel: Channel, deviceKey: String, message: AkkaMobileProtocol, sender: Option[ActorRef], connectionHandlingActor: ActorRef) {
    val request = new DefaultHttpRequest(
      HttpVersion.HTTP_1_1, HttpMethod.GET, uriString);
    request.setHeader(HttpHeaders.Names.HOST, host);
    request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
    request.setHeader(HttpHeaders.Names.CONTENT_TYPE,
      "application/x-www-form-urlencoded;charset=UTF-8");
    request.setHeader(HttpHeaders.Names.AUTHORIZATION, "GoogleLogin auth="
      + authToken);

    val messageBase64 = javax.xml.bind.DatatypeConverter.printBase64Binary(message.toByteArray)
    val postDataBuilder = new StringBuilder();
    postDataBuilder.append("registration_id").append("=")
      .append(deviceKey)
    postDataBuilder.append("&").append("collapse_key").append("=")
      .append("0");
    postDataBuilder.append("&").append("data.payload").append("=")
      .append(URLEncoder.encode(messageBase64, "UTF-8"));


    request.getContent.writeBytes(messageBase64.getBytes("UTF-8"))

    channel.write(request);

    channel.getCloseFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          errorHandler ! CouldNotConnectToC2MD(future.getCause,
            SendMessage(message, sender), connectionHandlingActor)
          future.getChannel.disconnect()
          future.getChannel.close()
        }
      }
    })
  }

  def close() {
    bootstrap.releaseExternalResources()
  }

  class HttpClientPipelineFactory extends ChannelPipelineFactory {
    def getPipeline: ChannelPipeline = {
      val pipeline = Channels.pipeline()
      val engine = SSLContext.getDefault.createSSLEngine()
      pipeline.addLast("ssl", new SslHandler(engine))
      pipeline.addLast("codec", new HttpClientCodec())
      // Remove the following line if you don't want automatic content decompression.
      pipeline.addLast("inflater", new HttpContentDecompressor())
      // Uncomment the following line if you don't want to handle HttpChunks.
      //pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
      pipeline.addLast("handler", new HttpResponseHandler(errorHandler))
      pipeline;
    }
  }

  class HttpResponseHandler(errorHandler: ActorRef) extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      val response = e.getMessage.asInstanceOf[HttpResponse];
      if (response.getStatus != HttpResponseStatus.OK) {
        EventHandler.notify(C2MDServerError("Get a unexpected HTTP-Response: " + response.getStatus, None))
      }
    }


    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      ctx.getChannel.close()
      EventHandler.notify(C2MDServerError("Unhandled exception in the server", Some(e.getCause)))
    }
  }

}
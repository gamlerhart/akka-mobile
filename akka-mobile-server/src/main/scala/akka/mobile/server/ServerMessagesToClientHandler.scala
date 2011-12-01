package akka.mobile.server

import java.util.concurrent.ConcurrentHashMap
import java.net.InetSocketAddress
import com.eaio.uuid.UUID
import akka.dispatch.CompletableFuture
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.mobile.communication._
import java.io.IOException
import akka.mobile.communication.CommunicationMessages.SendMessage
import org.jboss.netty.channel.{ChannelFuture, ChannelFutureListener, Channel => NettyChannel}
import akka.actor.{Actor, ActorRef}
import akka.mobile.communication.NetworkFailures.CannotSendDueNoConnection
import java.lang.String
import akka.mobile.server.C2MDCommunication.SendMessageAndUseC2MDFallback


/**
 *
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

class ServerMessagesToClientHandler(val registry: Registry,
                                    val errorHandler: ActorRef,
                                    fallbackSender: FallBackPushMessageSender) extends MessageSink {


  val serializer = new ServerSideSerialisation(this)
  private val clientChannels = new ConcurrentHashMap[ClientId, NettyChannel]()
  private val connectionManagingActors = new ConcurrentHashMap[ClientId, ActorRef]()
  private val futures = new FutureResultHandling
  val dispatcher = new WireMessageDispatcher(futures, registry, this, serializer)


  def registerFuture(uuid: UUID, future: CompletableFuture[Any]) {
    futures.put(uuid, future)
  }

  def sendResponse(clientId: Either[ClientId, InetSocketAddress],
                   responseFor: UUID, result: Right[Throwable, Any]) {

    val id = clientId.left.get;


    val msg = serializer.toWireProtocol(
      serializer.response(responseFor, result))

    sendWriteMessageToChannel(id, msg, None)
  }

  def send(clientId: Either[ClientId, InetSocketAddress], serviceId: Either[String, UUID],
           message: Any, sender: Option[ActorRef], replyUUID: Option[UUID]) {

    sender.foreach(si => {
      registry.registerActor("uuid:" + si.uuid.toString, si)
    })

    val msg = serializer.toWireProtocol(
      serializer.messageToActor(serviceId, sender, message, replyUUID))

    sendWriteMessageToChannel(clientId.left.get, msg, sender)
  }

  def connectionStateOf(clientId: ClientId): ClientConnectionState = {
    if (null == clientChannels.get(clientId)) {
      if (fallbackSender.canSendC2MDToThisClient(clientId)) {
        NotConnectedC2MDAvailable
      } else {
        NotConnectedNoC2MD
      }
    } else {
      Connected
    }
  }


  def connectionManagerFor(clientId: ClientId) = {
    val actor = connectionManagingActors.get(clientId)
    if (null == actor) {
      connectionManagingActors.putIfAbsent(clientId, Actor.actorOf(new ConnectionControlActor(clientId)).start())
      connectionManagingActors.get(clientId)
    } else {
      actor
    }
  }

  private[server] def putChannelForClient(client: ClientId, channel: NettyChannel)
  = clientChannels.put(client, channel)

  private[server] def removeChannelForClient(clientId: ClientId) = clientChannels.remove(clientId)

  private def sendWriteMessageToChannel(clientId: ClientId, msg: AkkaMobileProtocol, sender: Option[ActorRef],
                                        forceC2MD: C2MDForceOption = UseC2MDOnlyForSpecialMessages) {
    val backChannel = clientChannels.get(clientId)
    val connectionManager = connectionManagerFor(clientId);
    // in that case it has probably been closed
    if (null == backChannel) {
      sendOverC2MDOrFail(clientId, msg, sender, connectionManager, forceC2MD)
    } else {
      backChannel.write(msg).addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          if (!future.isCancelled && !future.isSuccess) {
            errorHandler ! CannotSendDueNoConnection(
              future.getCause,
              SendMessage(msg, None), connectionManager)
          }
        }
      });
    }
  }


  private def sendOverC2MDOrFail(clientId: ClientId, msg: AkkaMobileProtocol, sender: Option[ActorRef],
                                 connectionManager: ActorRef, forceC2MD: C2MDForceOption) {
    if (forceC2MD == AlwaysFallBackToC2MD) {
      fallbackSender.sendRequest(clientId, msg, sender, connectionManager)

    } else {
      val deserializedMsg = if (msg.hasMessage) {
        Some(serializer.deSerializeMsg(msg.getMessage, None)._1)
      } else {
        None
      }
      if (deserializedMsg.isDefined && deserializedMsg.get.isInstanceOf[SentThroughC2MDIfNoConnectionIsAvailable]) {
        fallbackSender.sendRequest(clientId, msg, sender, connectionManager)
      } else {
        errorHandler ! CannotSendDueNoConnection(
          new IOException("No channel available. Probably the connection has been closed"),
          SendMessage(msg, None), connectionManager)
      }

    }
  }


  class ConnectionControlActor(clientId: ClientId) extends Actor {
    protected def receive = {
      case SendMessage(msg, sender) => {
        sendWriteMessageToChannel(clientId, msg, sender, UseC2MDOnlyForSpecialMessages)
      }
      case SendMessageAndUseC2MDFallback(msg, sender) => {
        sendWriteMessageToChannel(clientId, msg, sender, AlwaysFallBackToC2MD)

      }
      case m: AkkaMobileControlMessage => {}
    }
  }


  sealed abstract class C2MDForceOption

  case object AlwaysFallBackToC2MD extends C2MDForceOption

  case object UseC2MDOnlyForSpecialMessages extends C2MDForceOption

}


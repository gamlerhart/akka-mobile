package akka.mobile.remote

import akka.actor._
import java.net.InetSocketAddress
import java.lang.IllegalArgumentException
import com.eaio.uuid.UUID

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */


trait RemoteClient {
  def actorFor(serviceId: String, hostname: String, port: Int): ActorRef

}

object MobileRemoteClient {

  /**
   * Creates a new instance.
   */
  def createClient(device: DeviceOperations) = new MobileRemoteClient(None, device.clientId)
}

class MobileRemoteClient(var messaging: Option[RemoteMessaging], clientId: ClientId) extends RemoteClient {

  val msgSink: MessageSink = new MessageSink() {

    def sendResponse(clientId: Either[ClientId, InetSocketAddress],
                     responseFor: UUID, result: Right[Throwable, Any]) = {
      throw new Error("TODO")
    }

    def send(clientId: Either[ClientId, InetSocketAddress],
             serviceId: String, message: Any,
             senderOption: Option[ActorRef],
             replyUUID: Option[UUID]) {
      clientId match {
        case Right(remoteAddress) => {
          messaging.get.registry.registerActor("uuid:" + senderOption.get.uuid.toString, senderOption.get)
          val theMessage = serializer.toWireProtocol(
            serializer.messageToActor(serviceId, senderOption, message, replyUUID))
          val remoteChannel = messaging.get.channelFor(remoteAddress)
          remoteChannel ! SendMessage(theMessage, senderOption)

        }
        case Left(_) => throw new IllegalArgumentException("Cannot send to a client from a client")
      }
    }
  }
  private val serializer = new ClientSideSerialisation(msgSink, clientId)

  if (messaging.isEmpty) {
    messaging = Some(RemoteMessaging(serializer, msgSink))
  }

  def actorFor(serviceId: String, hostname: String, port: Int) = {
    ClientRemoteActorRef(new InetSocketAddress(hostname, port), serviceId, msgSink)
  }

  def actorFor(serviceId: String, className: String, timeout: Long,
               hostname: String, port: Int, loader: Option[ClassLoader]) = {
    ClientRemoteActorRef(new InetSocketAddress(hostname, port), serviceId, msgSink)
  }

}




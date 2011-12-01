package akka.mobile.communication

import akka.actor.ActorRef
import java.net.InetSocketAddress
import akka.dispatch.CompletableFuture
import com.eaio.uuid.UUID

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait MessageSink {

  def ask(clientId: Either[ClientId, InetSocketAddress], serviceId: Either[String, UUID],
          message: Any, sender: Option[ActorRef], future: CompletableFuture[Any])
  : CompletableFuture[Any] = {
    val requestID = new UUID()
    registerFuture(requestID, future)
    send(clientId, serviceId, message, sender, Some(requestID))
    future;
  }

  def send(clientId: Either[ClientId, InetSocketAddress],
           serviceId: Either[String, UUID], message: Any,
           sender: Option[ActorRef]) {
    send(clientId, serviceId, message, sender, None)
  }

  def send(clientId: Either[ClientId, InetSocketAddress],
           serviceId: Either[String, UUID], message: Any,
           sender: Option[ActorRef], replyUUID: Option[UUID]): Unit

  def sendResponse(nodeId: Either[ClientId, InetSocketAddress], responseFor: UUID, result: Right[Throwable, Any])

  def registerFuture(uuid: UUID, future: CompletableFuture[Any])
}
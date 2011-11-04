package akka.mobile.remote

import akka.actor.ActorRef
import java.net.InetSocketAddress
import akka.dispatch.CompletableFuture
import com.eaio.uuid.UUID

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait MessageSink {
  def sendResponse(uuid: UUID, result: Right[Throwable, Any]) {
    throw new Error("TODO")
  }

  def ask(clientId: Right[Nothing, InetSocketAddress], serviceId: String,
          message: Any, sender: Option[ActorRef], future: CompletableFuture[Any])
  : CompletableFuture[Any] = {
    val requestID = new UUID()
    registerFuture(requestID, future)
    send(clientId, serviceId, message, sender, Some(requestID))
    future;
  }

  def send(clientId: Either[ClientId, InetSocketAddress],
           serviceId: String, message: Any,
           sender: Option[ActorRef]) {
    send(clientId, serviceId, message, sender, None)
  }

  def send(clientId: Either[ClientId, InetSocketAddress],
           serviceId: String, message: Any,
           sender: Option[ActorRef], replyUUID: Option[UUID]): Unit

  def sendResponse(clientId: Either[ClientId, InetSocketAddress], responseFor: UUID, result: Right[Throwable, Any])

  def registerFuture(uuid: UUID, future: CompletableFuture[Any])
}
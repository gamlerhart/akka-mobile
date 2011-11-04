package akka.mobile.remote

import akka.actor.ActorRef
import java.net.InetSocketAddress
import akka.dispatch.{CompletableFuture, ActorCompletableFuture}
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
          message: Any, sender: Option[ActorRef], future: Option[ActorCompletableFuture]): CompletableFuture[Any]
  = {
    val r = future.getOrElse(new ActorCompletableFuture)
    send(clientId, serviceId, message, sender, Some(new UUID()))
    r
  }

  def send(clientId: Either[ClientId, InetSocketAddress],
           serviceId: String, message: Any,
           sender: Option[ActorRef]) {
    send(clientId, serviceId, message, sender, None)
  }

  def send(clientId: Either[ClientId, InetSocketAddress],
           serviceId: String, message: Any,
           sender: Option[ActorRef], replyUUID: Option[UUID]): Unit

}
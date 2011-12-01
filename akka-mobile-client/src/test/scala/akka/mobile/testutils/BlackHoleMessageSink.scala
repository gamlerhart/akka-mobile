package akka.mobile.testutils

import akka.mobile.communication.{ClientId, MessageSink}
import java.net.InetSocketAddress
import akka.actor.ActorRef
import com.eaio.uuid.UUID
import akka.dispatch.CompletableFuture


/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

object BlackHoleMessageSink extends MessageSink {
  def send(clientId: Either[ClientId, InetSocketAddress], serviceId: Either[String, UUID], message: Any,
           sender: Option[ActorRef], replyId: Option[UUID]) {

  }

  def sendResponse(clientId: Either[ClientId, InetSocketAddress], responseFor: UUID, result: Right[Throwable, Any]) {

  }

  def registerFuture(uuid: UUID, future: CompletableFuture[Any]) {}
}
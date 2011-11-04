package akka.mobile.testutils

import akka.mobile.remote.{ClientId, MessageSink}
import java.net.InetSocketAddress
import akka.actor.ActorRef
import com.eaio.uuid.UUID


/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

object BlackHoleMessageSink extends MessageSink {
  def send(clientId: Either[ClientId, InetSocketAddress], serviceId: String, message: Any,
           sender: Option[ActorRef], replyId: Option[UUID]) = {

  }

  def sendResponse(clientId: Either[ClientId, InetSocketAddress], responseFor: UUID, result: Right[Throwable, Any]) {

  }
}
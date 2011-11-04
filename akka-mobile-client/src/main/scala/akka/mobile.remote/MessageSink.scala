package akka.mobile.remote

import akka.actor.ActorRef
import java.net.InetSocketAddress
import akka.dispatch.{CompletableFuture, ActorCompletableFuture}

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait MessageSink {
  def ask(clientId: Right[Nothing, InetSocketAddress], serviceId: String,
          message: Any, sender: Option[ActorRef], future: Option[ActorCompletableFuture]): CompletableFuture[Any]
  = {
    throw new Error("Todo")
  }

  def send(clientId: Either[ClientId, InetSocketAddress], serviceId: String, message: Any, sender: Option[ActorRef])

}
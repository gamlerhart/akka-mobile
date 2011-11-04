package akka.mobile.remote

import akka.actor.ActorRef
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait MessageSink {
  def send(clientId: Either[ClientId, InetSocketAddress], serviceId: String, message: Any, sender: Option[ActorRef])

}
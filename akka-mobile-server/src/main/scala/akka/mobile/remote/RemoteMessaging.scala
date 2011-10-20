package akka.mobile.remote

import akka.actor.ActorRef

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

private[mobile] trait RemoteMessaging {
  def send(clientId: ClientId, serviceId: String, value: Any, option: Option[ActorRef]): Unit

}
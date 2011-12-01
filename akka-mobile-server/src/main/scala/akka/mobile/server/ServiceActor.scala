package akka.mobile.server

import akka.actor.Actor
import akka.mobile.communication.ClientId

/**
 * @author roman.stoffel@gamlor.info
 * @since 18.11.11
 */

trait ServiceActor {
  this: Actor =>

  def clientId: Option[ClientId] = {
    self.sender.map(s => s.asInstanceOf[RemoteDeviceActorRef].clientId)
  }

}
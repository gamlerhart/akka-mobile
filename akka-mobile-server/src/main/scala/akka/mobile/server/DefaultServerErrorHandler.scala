package akka.mobile.server

import akka.actor.Actor

/**
 * @author roman.stoffel@gamlor.info
 * @since 11.11.11
 */

object DefaultServerErrorHandler {
  def apply() = Actor.actorOf(new Actor() {
    protected def receive = {
      case ohoh => {
      }
    }
  }).start()

}
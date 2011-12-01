package akka.mobile.client

import akka.actor.Actor
import akka.mobile.communication.RetryAfterTimeout

/**
 * @author roman.stoffel@gamlor.info
 * @since 10.11.11
 */


object DefaultClientErrorHandler {
  def apply() = Actor.actorOf(new RetryAfterTimeout()).start()


}
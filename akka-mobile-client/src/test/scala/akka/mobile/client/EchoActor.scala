package akka.mobile.client

import akka.actor.Actor

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

class EchoActor extends Actor {
  protected def receive = {
    case msg => self.reply(msg)
  }
}
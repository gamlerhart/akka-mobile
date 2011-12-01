package akka.mobile.testutils

import akka.actor.Actor

/**
 * @author roman.stoffel@gamlor.info
 * @since 19.11.11
 */


class EchoActor extends Actor {
  protected def receive = {
    case msg => self.reply(msg)
  }
}
package akka.mobile.communication

import akka.actor.{Actor, ActorRef}


/**
 * @author roman.stoffel@gamlor.info
 * @since 17.11.11
 */

object BroadcastTo {
  def apply(receivers: Seq[ActorRef]): ActorRef = {
    Actor.actorOf(new BroadcastTo(receivers)).start()
  }


}

class BroadcastTo(receivers: Seq[ActorRef]) extends Actor {
  protected def receive = {
    case msg => {
      receivers.foreach(_.forward(msg))
    }
  }
}
package akka.mobile.testutils

import akka.actor.Actor


/**
 * @author roman.stoffel@gamlor.info
 * @since 10.11.11
 */

/**
 * Any message sent to this actor just disappears
 */
object BlackholeActor {
  private val blackHole = Actor.actorOf[BlackHole].start()

  def apply() = blackHole

  class BlackHole extends Actor {
    protected def receive = {
      case ignored => {}
    }

  }

}
package akka.mobile.communication

import akka.actor.Actor
import akka.mobile.communication.RespondFailureBackToSender.FailedToSend
import akka.mobile.communication.NetworkFailures.CommunicationError

/**
 * @author roman.stoffel@gamlor.info
 * @since 10.11.11
 */

class RespondFailureBackToSender extends Actor {
  protected def receive = {
    case cf: CommunicationError => {
      cf.tryGetMessages.filter(i => i._2.isDefined)
        .filter(i => i._1.isDefined)
        .foreach(i => {
        i._2.get ! FailedToSend(i._1.get, cf.lastException)
      })

    }
  }
}

object RespondFailureBackToSender {


  case class FailedToSend(message: Any, lastError: Throwable)

}
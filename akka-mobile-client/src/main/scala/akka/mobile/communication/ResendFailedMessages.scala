package akka.mobile.communication

import akka.actor.Actor
import collection.mutable.Queue
import akka.mobile.communication.CommunicationMessages._
import akka.mobile.communication.NetworkFailures._

/**
 * @author roman.stoffel@gamlor.info
 * @since 12.11.11
 */


class ResendFailedMessages() extends Actor {
  private val queue = Queue[SendMessage]()

  protected def receive = {
    case error: CommunicationError => {
      queue.++=(error.unsentMessages)
    }
    case StartedConnecting(connection) => {
      queue.foreach(msg => {
        connection ! msg
      })
    }
    case ignored: AkkaMobileControlMessage => {}
  }
}
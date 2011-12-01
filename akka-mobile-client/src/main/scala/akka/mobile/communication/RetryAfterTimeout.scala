package akka.mobile.communication

import akka.util.Duration
import akka.util.duration._
import akka.actor.{Scheduler, Actor}
import akka.mobile.communication.NetworkFailures._
import java.util.concurrent.TimeUnit

/**
 * @author roman.stoffel@gamlor.info
 * @since 11.11.11
 */

class RetryAfterTimeout(duration: Duration = 2.seconds, scaleFactor: Double = 1.5, maxRetries: Int = 5) extends Actor {
  protected def receive = {
    case f: ConnectionError => {
      become({
        case TimeOutOver(lastDuration, 0) => {
          unbecome()
        }
        case StartedConnecting(connection) => {
          unbecome()
        }
        case TimeOutOver(lastDuration, retriesLeft) => {
          f.connectionManagingActor ! PleaseTryToReconnect

          val newTimeout = (lastDuration.toMillis * scaleFactor).toLong
          Scheduler.scheduleOnce(self, TimeOutOver(Duration(newTimeout, TimeUnit.MILLISECONDS), retriesLeft - 1), newTimeout, TimeUnit.MILLISECONDS)
        }
      })
      Scheduler.scheduleOnce(self, TimeOutOver(duration, maxRetries), duration.length, duration.unit)
    }
    case TimeOutOver(_, _) => {
      // not interesting anymore
    }
    case x: AkkaMobileControlMessage => {
    }
  }

  case class TimeOutOver(lastDuration: Duration, retriesLeft: Int)

}
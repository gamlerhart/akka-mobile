package info.gamlor.playaround

import akka.actor.Actor

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

object PlayWithAkka extends App {
  main()

  def main() {
    val echoActor = Actor.actorOf[JustReplyActor].start()
    val anActor = Actor.actorOf(new Actor() {

      override def preStart() {
        self ! "Start"
      }

      protected def receive = {
        case "Start" => {
          echoActor ! "HiThere"
        }
      }
    }).start()
  }
}

class JustReplyActor extends Actor {
  protected def receive = {
    case x => self.reply(x)
  }
}


package info.gamlor.akkamobile

import akka.actor.Actor
import akka.actor.newUuid
import akka.mobile.remote.MobileRemoteClient

/**
 * @author roman.stoffel@gamlor.info
 * @since 05.10.11
 */

class MyActor(remote: MobileRemoteClient) extends Actor {
  self.id = newUuid().toString

  protected def receive = {
    case "Start" => {
      val actor = remote.actorFor("echo", "152.96.235.59", 2552);



      actor ! "hi from droid"
    }
    case x: String => {
      println(x)
    }
  }
}
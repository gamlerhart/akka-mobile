package info.gamlor.akkamobile

import akka.actor.Actor
import akka.actor.newUuid
import android.util.Log
import android.os.Handler
import android.widget.TextView
import android.content.Context
import android.app.Activity
import akka.mobile.remote.MobileRemoteClient

/**
 * @author roman.stoffel@gamlor.info
 * @since 05.10.11
 */

class MyActor() extends Actor {
  self.id = newUuid().toString

  protected def receive = {
    case "Start" => {
      val remote = MobileRemoteClient.client;
      val actor = remote.actorFor("echo", "10.0.2.2", 2552);



      actor ! "hi from droid"
    }
    case x: String => {
      println(x)
    }
  }
}
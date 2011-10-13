package info.gamlor.akkamobile

import akka.actor.Actor
import akka.actor.newUuid
import android.util.Log
import android.os.Handler
import android.widget.TextView
import android.content.Context
import android.app.Activity


/**
 * @author roman.stoffel@gamlor.info
 * @since 05.10.11
 */

class MyActor(val handler: Handler, val ctx: Activity) extends Actor {
  self.id = newUuid().toString
  protected def receive = {
    case "Start" =>{
    }
    case x : String => {
    }
  }
}
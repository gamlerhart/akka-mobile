package info.gamlor.akkamobile

import android.app.Activity
import android.os.Bundle
import akka.actor.Actor
import akka.mobile.android.{ActivityActor, AndroidDevice}
import android.util.Log
import akka.util.ReflectiveAccess

class AkkaDroidApp extends Activity with ActivityActor {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    try {
      val t = ReflectiveAccess.getClassFor[Actor]("akka.mobile.android.LogcatLogger")
      val actor = Actor.actorOf(t.right.get)
      Log.i("i", actor.toString)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
      }
    }

    val otherGuy = Actor.actorOf[OtherTestActor].start()
    otherGuy ! "Start"
  }

  protected def receive = {
    case "Answer" => {
      Log.i("Ac", "a")
    }
  }
}

class OtherTestActor extends Actor {

  protected def receive = {
    case "Start" => {
      throw new Exception("Oh Boy")
    }
  }
}


package info.gamlor.akkamobile

import android.app.Activity
import android.os.Bundle
import akka.actor.Actor
import akka.mobile.android.{ActivityActor, AndroidDevice}
import android.util.Log

class AkkaDroidApp extends Activity with ActivityActor {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

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
      self.reply("Answer")
    }
  }
}


package info.gamlor.akkamobile

import android.app.Activity
import android.os.Bundle
import akka.actor.Actor
import akka.config.Config
import android.util.Log

class AkkaDroidApp extends Activity {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)


    val cfgProv = Config.config;

    val cfg = cfgProv.getString("akka.remote.layer","noNo");
    Log.i("cfg",cfg)
    val r = Actor.actorOf[MyActor].start()
    r ! "Start"
  }

}


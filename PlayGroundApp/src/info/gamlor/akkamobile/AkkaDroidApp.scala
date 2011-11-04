package info.gamlor.akkamobile

import android.app.Activity
import android.os.Bundle
import akka.actor.Actor
import akka.mobile.remote.MobileRemoteClient
import akka.mobile.android.AndroidDevice

class AkkaDroidApp extends Activity {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    val remote = MobileRemoteClient.createClient(AndroidDevice(this));


    val r = Actor.actorOf(new MyActor(remote)).start()
    r ! "Start"
  }

}


package info.gamlor.akkamobile

import android.app.Activity
import android.os.Bundle
import akka.actor.Actor
import akka.config.Config
import android.util.Log
import android.telephony.TelephonyManager
import android.content.Context
import android.provider.Settings.Secure

class AkkaDroidApp extends Activity {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    val telephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE).asInstanceOf[TelephonyManager];

    val id = telephonyManager.getDeviceId;
    println(id);

    val otehrId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
    println(id);

    val r = Actor.actorOf[MyActor].start()
    r ! "Start"
  }

}


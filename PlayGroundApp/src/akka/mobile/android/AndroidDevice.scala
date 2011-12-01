package akka.mobile.android

import android.provider.Settings.Secure
import akka.mobile.client.DeviceOperations
import akka.mobile.communication.ClientId
import android.preference.PreferenceManager
import android.content.{Intent, SharedPreferences, Context}
import android.app.{PendingIntent, Activity}

/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class AndroidDevice(context: Context, appID: String) extends DeviceOperations {

  val clientId = ClientId(
    Secure.getString(context.getContentResolver, Secure.ANDROID_ID), appID)

  def getPreference(key: String, defaultValue: String): String = {
    val pref = resources()
    pref.getString(key, defaultValue)
  }

  def getPreference(key: String, defaultValue: Int): Int = {
    val pref = resources()
    pref.getInt(key, defaultValue)
  }


  def storePreference(writeClosure: (PropertyWriteContext) => Unit) {
    val editor = resources().edit()
    writeClosure(new PropertyWriteContext() {
      def put(key: String, valueToSet: String) {
        editor.putString(key, valueToSet)
      }

      def put(key: String, valueToSet: Int) {
        editor.putInt(key, valueToSet)
      }
    })
    editor.commit()
  }

  def registerForC2MD(forEmail: String) {
    val intent = new Intent("com.google.android.c2dm.intent.REGISTER");
    intent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
    intent.putExtra("sender", "youruser@gmail.com");
    context.startService(intent);
  }


  private def resources(): SharedPreferences = {
    context.getSharedPreferences("akka.mobile.preferences", Context.MODE_PRIVATE)
  }
}

object AndroidDevice {
  def apply(context: Context)
  = new AndroidDevice(context.getApplicationContext, context.getApplicationContext.getPackageName)

  def apply(context: Context, appID: String)
  = new AndroidDevice(context.getApplicationContext, appID)
}
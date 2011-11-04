package akka.mobile.android

import android.content.Context
import akka.mobile.remote.{ClientId, DeviceOperations}
import android.provider.Settings.Secure

/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class AndroidDevice(context: Context, appID: String) extends DeviceOperations {

  val clientId = ClientId(
    Secure.getString(context.getContentResolver, Secure.ANDROID_ID), appID)
}

object AndroidDevice {
  def apply(context: Context)
  = new AndroidDevice(context.getApplicationContext, context.getApplicationContext.getPackageName)

  def apply(context: Context, appID: String)
  = new AndroidDevice(context.getApplicationContext, appID)
}
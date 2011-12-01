package info.gamlor.akkamobile

import android.content.{Intent, Context, BroadcastReceiver}
import android.util.Log
import akka.mobile.android.C2MDReceiver


/**
 * @author roman.stoffel@gamlor.info
 * @since 18.11.11
 */

class DispatchToAkka extends C2MDReceiver {
  def remoteClient(context: Context) = {
    context.getApplicationContext.asInstanceOf[MyApplication].remote;
  }
}
package info.gamlor.akkamobile

import akka.mobile.client.MobileRemoteClient
import akka.actor.Actor._
import akka.mobile.communication.{BroadcastTo, ResendFailedMessages, RetryAfterTimeout}
import akka.mobile.android.AndroidDevice

/**
 * @author roman.stoffel@gamlor.info
 * @since 17.11.11
 */

class MyApplication extends android.app.Application {

  lazy val remote = MobileRemoteClient.createClient(
    AndroidDevice(this),
    Some(("10.78.74.17", 2552)),
    errorHandler = BroadcastTo(Seq(actorOf(new RetryAfterTimeout()).start(), actorOf(new ResendFailedMessages()).start())))

  override def onCreate() {
    println("On Create")
  }

  override def onTerminate() {
    println("On Create")
  }
}

object MyApplication {
  implicit def toBitmapData(app: android.app.Application): MyApplication = {
    app.asInstanceOf[MyApplication]
  }

}
package akka.mobile.android

import akka.actor.Actor
import akka.event.EventHandler.{Debug, Info, Warning, Error}
import akka.event.EventHandler
import android.util.Log
import java.text.DateFormat
import java.util.Date

/**
 * @author roman.stoffel@gamlor.info
 * @since 07.11.11
 */

class LogcatLogger extends Actor {

  import EventHandler._

  self.id = "LogCat-Logger"
  self.dispatcher = EventHandlerDispatcher

  def formattedTimestamp = DateFormat.getInstance.format(new Date)

  def receive = {
    case event@Error(cause, instance, message) =>
      Log.e("AkkaMobile-Error", error.format(
        formattedTimestamp,
        event.thread.getName,
        instance.getClass.getSimpleName,
        message, ""), cause)
    case event@Warning(instance, message) =>
      Log.w("AkkaMobile-Warning", warning.format(
        formattedTimestamp,
        event.thread.getName,
        instance.getClass.getSimpleName,
        message))
    case event@Info(instance, message) =>
      Log.i("AkkaMobile-Info", info.format(
        formattedTimestamp,
        event.thread.getName,
        instance.getClass.getSimpleName,
        message))
    case event@Debug(instance, message) =>
      Log.d("AkkaMobile-Debug", debug.format(
        formattedTimestamp,
        event.thread.getName,
        instance.getClass.getSimpleName,
        message))
    case event ⇒
      Log.d("AkkaMobile", generic.format(formattedTimestamp, event.toString))
  }
}
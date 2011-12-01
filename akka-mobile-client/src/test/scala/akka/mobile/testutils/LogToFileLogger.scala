package akka.mobile.testutils

import akka.actor.Actor
import akka.event.EventHandler
import java.text.DateFormat
import java.util.Date
import java.io.{File, FileWriter, PrintWriter}

/**
 * @author roman.stoffel@gamlor.info
 * @since 11.11.11
 */

class LogToFileLogger extends Actor {

  import EventHandler._

  var writer: PrintWriter = null
  self.id = "LogCat-Logger"
  self.dispatcher = EventHandlerDispatcher

  def formattedTimestamp = DateFormat.getInstance.format(new Date)


  override def preStart() {
    new File("tests-log.log").delete()
    writer = new PrintWriter(new FileWriter("tests-log.log"))
  }

  def receive = {
    case event@Error(cause, instance, message) =>
      writer.write("======AkkaMobile-Error======\n"
        + error.format(
        formattedTimestamp,
        event.thread.getName,
        instance.getClass.getSimpleName,
        message, ""))
      cause.printStackTrace(writer)
      writer.flush()
    case event@Warning(instance, message) =>
      writer.write("======AkkaMobile-Warning======\n"
        + error.format(
        formattedTimestamp,
        event.thread.getName,
        instance.getClass.getSimpleName,
        message, ""))
      writer.flush()
    case event@Info(instance, message) =>
      writer.write("======AkkaMobile-Info======\n"
        + error.format(
        formattedTimestamp,
        event.thread.getName,
        instance.getClass.getSimpleName,
        message, ""))
      writer.flush()
    case event@Debug(instance, message) =>
      writer.write("======AkkaMobile-Debug======\n"
        + error.format(
        formattedTimestamp,
        event.thread.getName,
        instance.getClass.getSimpleName,
        message, ""))
      writer.flush()
    case event ⇒
      writer.write("======AkkaMobile======\n" + generic.format(formattedTimestamp, event.toString))
  }
}
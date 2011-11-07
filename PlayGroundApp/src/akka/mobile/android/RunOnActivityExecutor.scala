package akka.mobile.android

import android.os.Handler
import java.util.concurrent.{TimeUnit, AbstractExecutorService}

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

object RunOnActivitiyDispatcher {
  def apply() = new RunOnActivitiyDispatcher(new UnboundedMailbox)
}

class RunOnActivitiyDispatcher(_mailboxType: MailboxType)
  extends ExecutorBasedEventDrivenDispatcher("Activity-Dispatcher",
    Dispatchers.THROUGHPUT, -1, _mailboxType, ThreadBasedDispatcher.oneThread
  ) {
  executorService.set(new RunOnActivityExecutor())
}

class RunOnActivity extends ThreadPoolConfig {

}

class RunOnActivityExecutor extends AbstractExecutorService {
  val handler = new Handler()

  def notSupported[T](): T = {
    throw new UnsupportedOperationException("Cannot shut down activity executor service")
  }

  def shutdown() {
    notSupported()
  }

  def shutdownNow() = notSupported()

  def isShutdown = false

  def isTerminated = false

  def awaitTermination(timeout: Long, unit: TimeUnit) = notSupported()

  def execute(command: Runnable) {
    handler.post(command)
  }
}
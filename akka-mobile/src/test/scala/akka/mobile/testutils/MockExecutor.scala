package akka.mobile.testutils

import java.util.Collection
import java.lang.UnsupportedOperationException
import java.util.concurrent.{Future, TimeUnit, Callable, ExecutorService}

/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */

class MockExecutor extends ExecutorService {
  private val queue = scala.collection.mutable.Queue[Callable[Any]]()

  def shutdown() {
    notSupported()
  }

  def shutdownNow() = notSupported()

  def isShutdown = notSupported()

  def isTerminated = notSupported()

  def awaitTermination(timeout: Long, unit: TimeUnit) = notSupported()

  def submit[T](task: Callable[T]) = notSupported()

  def submit[T](task: Runnable, result: T) = notSupported()

  override def submit(task: Runnable): Future[_] = {
    queue.enqueue(new Callable[Any] {
      def call() {
        task.run();
        ""
      }
    });
    null;
  }

  def invokeAll[T](tasks: Collection[_ <: Callable[T]]) = notSupported()

  def invokeAll[T](tasks: Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit) = notSupported()

  def invokeAny[T](tasks: Collection[_ <: Callable[T]]) = notSupported()

  def invokeAny[T](tasks: Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit) = notSupported()

  def execute(command: Runnable) = notSupported()

  def executeLastItemNow() {
    queue.dequeue().call()
  }

  private def notSupported[T]() = {
    throw new UnsupportedOperationException("Not supported")
  }
}
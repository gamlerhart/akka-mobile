package akka.mobile.communication

import com.eaio.uuid.UUID
import java.util.concurrent.ConcurrentHashMap
import akka.dispatch.CompletableFuture

/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class FutureResultHandling {

  private val futures = new ConcurrentHashMap[UUID, CompletableFuture[_]]()

  def completeWithError(uuid: UUID, exception: Exception) {
    val f = futures.remove(uuid);
    if (f ne null) {
      f.asInstanceOf[CompletableFuture[Any]].completeWithException(exception)
    }


  }

  def completeWithResult(uuid: UUID, data: Any) {
    val f = futures.remove(uuid);
    if (f ne null) {
      f.asInstanceOf[CompletableFuture[Any]].completeWithResult(data)
    }
  }

  def put(uuid: UUID, future: CompletableFuture[_]) {
    futures.putIfAbsent(uuid, future)
    future.onTimeout(f => futures.remove(uuid))
  }

}
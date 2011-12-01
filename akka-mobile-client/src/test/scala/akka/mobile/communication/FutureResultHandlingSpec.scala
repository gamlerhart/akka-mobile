package akka.mobile.communication

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import com.eaio.uuid.UUID
import akka.dispatch.{FutureTimeoutException, DefaultCompletableFuture}

/**
 * @author roman.stoffel@gamlor.info
 * @since 04.11.11
 */

class FutureResultHandlingSpec extends Spec with ShouldMatchers {

  describe("FutureResultHandling") {
    it("should keep future for uuid") {
      val toTest = new FutureResultHandling()
      val id = new UUID()
      val future = new DefaultCompletableFuture[String]()
      toTest.put(id, future)

      toTest.completeWithResult(id, "Data");

      future.isCompleted should be(true)
      future.get should be("Data")
    }
    it("cleans up unused futures") {
      val toTest = new FutureResultHandling()
      val id = new UUID()
      val future = new DefaultCompletableFuture[String]()
      toTest.put(id, future)

      intercept[FutureTimeoutException] {
        future.await
      }

      future.isExpired should be(true)
      toTest.completeWithResult(id, "Data");
    }
    it("fails") {
      val toTest = new FutureResultHandling()
      val id = new UUID()
      val future = new DefaultCompletableFuture[String]()
      toTest.put(id, future)


      toTest.completeWithError(id, new Exception());

      future.isCompleted should be(true)
      toTest.completeWithResult(id, "Data");
    }
  }

}
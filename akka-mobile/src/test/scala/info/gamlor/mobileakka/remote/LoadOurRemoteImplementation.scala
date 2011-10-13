package info.gamlor.mobileakka.remote

import org.scalatest.Spec
import akka.actor.{Actor, Actors}
import org.scalatest.matchers.ShouldMatchers._

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

class LoadOurRemoteImplementation  extends Spec {

  describe("Akka") {

    it("should load our remote implementation") {

      val remote = Actor.remote.start()

      remote should not be (null)
    }

  }
}
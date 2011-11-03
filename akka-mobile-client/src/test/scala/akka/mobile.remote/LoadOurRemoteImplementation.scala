package akka.mobile

import akka.actor.Actor
import org.scalatest.matchers.ShouldMatchers._
import org.scalatest.{GivenWhenThen, Spec}

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

class LoadOurRemoteImplementation  extends Spec{
  describe("Akka") {

    it("should load our remote implementation") {

      val remote = Actor.remote.start()

      remote should not be (null)
    }
  }
}
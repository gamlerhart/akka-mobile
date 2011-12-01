package akka.mobile.communication

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.actor.Actor
import akka.mobile.client.EchoActor

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

class ActorRegistrySpec extends Spec with ShouldMatchers {


  describe("On the Actor Registry") {

    it("you can register and get an actor") {
      val reg = new Registry()

      val testActor = Actor.actorOf[EchoActor];
      reg.registerActor("my-actor", testActor)
      val actorFromRegistry = reg.findActorById("my-actor")

      actorFromRegistry should be(testActor)
    }

    it("you can register different actors") {
      val reg = new Registry()

      val a1 = Actor.actorOf[EchoActor];
      val a2 = Actor.actorOf[EchoActor];
      reg.registerActor("actor1", a1)
      reg.registerActor("actor2", a2)
      val actorFromRegistry1 = reg.findActorById("actor1")
      val actorFromRegistry2 = reg.findActorById("actor2")

      actorFromRegistry1 should be(a1)
      actorFromRegistry2 should be(a2)
    }
    it("you cannot register different actors with the same id") {
      val reg = new Registry()

      val a1 = Actor.actorOf[EchoActor];
      reg.registerActor("actor", a1)

      val a2 = Actor.actorOf[EchoActor];

      intercept[IllegalStateException] {
        reg.registerActor("actor", a2)
      }
    }
    it("you can register the same actor with the same id") {
      val reg = new Registry()

      val a1 = Actor.actorOf[EchoActor];
      reg.registerActor("actor", a1)

      reg.registerActor("actor", a1)
    }

  }

}
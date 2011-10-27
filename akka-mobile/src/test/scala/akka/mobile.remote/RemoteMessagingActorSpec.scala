package akka.mobile.remote

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.actor.Actor
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.testkit.TestKit
import akka.util.Duration
import java.io.IOException

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.10.11
 */

class RemoteMessagingActorSpec extends Spec with ShouldMatchers with TestKit {

  class ThrowOnSend extends RemoteMessageChannel(new MockSocket) {
    override def send(msg: AkkaMobileProtocol) {
      throw new IOException("write failed")
    }
  }

  val msg = SendMessage(null, None);

  describe("On IOException") {
    //    it("it reports it back") {
    //      val actor = Actor.actorOf(new RemoteMessagingActor(()=>new ThrowOnSend())).start()
    //      actor ! msg;
    //
    //      val response = receiveOne(Duration.Inf)
    //      response match {
    //        case SendingFailed(x,m) =>{
    //          m should be (msg)
    //          x.getMessage should be ("write failed")
    //        }
    //        case _ => fail("Didn't get expected message")
    //      }
    //    }
    it("closes and fails") {
      var instanceCreationCounter = 0;
      var closedOldChannel = false;
      val channel = new ThrowOnSend() {
        override def close() {
          super.close()
          closedOldChannel = true;
        }
      }
      val actor = Actor.actorOf(new RemoteMessagingActor(() => {
        instanceCreationCounter = instanceCreationCounter + 1
        channel
      })).start()

      actor ! msg

      receiveOne(Duration.Inf)

      closedOldChannel should be(true)
    }

  }
}
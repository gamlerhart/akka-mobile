package akka.mobile.client

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import java.io.IOException
import akka.mobile.communication.NetworkFailures._
import akka.testkit.TestProbe
import akka.mobile.testutils._
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

/**
 * @author roman.stoffel@gamlor.info
 * @since 10.11.11
 */

class FailureHandlingSpec extends Spec with ShouldMatchers {

  import akka.util.duration._

  implicit val sender = BlackholeActor

  def ignoreConnectionMsgProbe() = {
    val probe = TestProbe()
    probe.ignoreMsg({
      case x: StartedConnecting => true
      case _ => false
    })
    probe
  }

  describe("Error on Client") {
    it("notifies error handler actor") {
      val probe = ignoreConnectionMsgProbe()
      val client = MobileRemoteClient.createClient(TestDevice(),
        socketFactory = a => MockSockets.ThrowOnAccess,
        errorHandler = probe.ref)

      val remoteActor = client.actorFor("not-existing", "not-existing.localhost", 1337)

      remoteActor.!("SendMsg1")(probe.ref)

      val msg = probe.receiveOne(5.seconds).asInstanceOf[ConnectionError]
      msg.lastException.isInstanceOf[IOException] should be(true)
    }
    it("get messages for failed sending back") {

      val probe = ignoreConnectionMsgProbe()
      val client = MobileRemoteClient.createClient(TestDevice(),
        socketFactory = a => MockSockets.ThrowOnAccess,
        errorHandler = probe.ref)

      val remoteActor = client.actorFor("not-existing", "not-existing.localhost", 1337)

      remoteActor ! ("SendMsg1")
      remoteActor ! ("SendMsg2")
      remoteActor ! ("SendMsg3")

      val msgs = probe.receiveOne(10.seconds).asInstanceOf[ConnectionError].unsentMessages
      msgs.size should be >= (1)
      msgs.size should be <= (3)
    }
    it("after failure we get connection not available") {

      val probe = ignoreConnectionMsgProbe()
      val client = MobileRemoteClient.createClient(TestDevice(),
        socketFactory = a => MockSockets.ThrowOnAccess,
        errorHandler = probe.ref)

      val remoteActor = client.actorFor("not-existing", "not-existing.localhost", 1337)

      remoteActor ! "FailWithMe"

      probe.receiveOne(10.seconds) should not be (null)

      remoteActor ! "AfterFailure"

      val nextFailure = probe.receiveOne(10.seconds).asInstanceOf[CannotSendDueNoConnection]

      nextFailure.trySentGetMessage().get should be("AfterFailure")
    }
    it("can restart connection process") {
      val probe = ignoreConnectionMsgProbe()

      val counter = new AtomicInteger(0)
      val client = MobileRemoteClient.createClient(TestDevice(),
        socketFactory =
          a => {
            counter.incrementAndGet()
            MockSockets.ThrowOnAccess
          },
        errorHandler = probe.ref)

      val remoteActor = client.actorFor("not-existing", "not-existing.localhost", 1337)

      remoteActor ! "FailWithMe"

      val networManagingActor = probe.receiveOne(5.seconds).asInstanceOf[CommunicationError].connectionManagingActor

      val amountTries = counter.get()

      probe.receiveOne(1.seconds)

      networManagingActor ! PleaseTryToReconnect

      probe.receiveN(1, 3.seconds)

      remoteActor ! "FailWithMe"

      val nextFailure = probe.receiveN(1, 3.seconds)

      counter.get() should be > amountTries
    }
    it("notifies when restarting") {
      val probe = TestProbe()
      val client = MobileRemoteClient.createClient(TestDevice(),
        socketFactory =
          a => {
            MockSockets.ThrowOnAccess
          },
        errorHandler = probe.ref)

      val remoteActor = client.actorFor("not-existing", "not-existing.localhost", 1337)
      remoteActor ! "FailWithMe"
      probe.receiveOne(3.seconds).asInstanceOf[StartedConnecting] should not be (null)
      val networManagingActor = probe.receiveOne(3.seconds).asInstanceOf[CommunicationError].connectionManagingActor

      networManagingActor.!(PleaseTryToReconnect)(probe.ref)

      probe.expectMsg(StartedConnecting(networManagingActor))
    }
    it("restarting works when still no connection can be built") {
      val errorHandler = TestProbe()
      val canCrashUntilZero = new AtomicInteger(Integer.MAX_VALUE)
      val client = MobileRemoteClient.createClient(TestDevice(),
        socketFactory =
          a => {
            if (0 >= canCrashUntilZero.decrementAndGet()) {
              new MockSocket()
            } else {
              MockSockets.ThrowOnAccess
            }
          },
        errorHandler = errorHandler.ref)

      val remoteActor = client.actorFor("not-existing", "not-existing.localhost", 1337)

      remoteActor ! "FailWithMe"
      errorHandler.expectMsgPF(1.seconds) {
        case StartedConnecting(_) => {
          true
        }
      }
      val networManagingActor = errorHandler.receiveOne(10.seconds).asInstanceOf[ConnectionError].connectionManagingActor
      canCrashUntilZero.set(1)
      networManagingActor.!(PleaseTryToReconnect)(errorHandler.ref)

      errorHandler.expectMsg(StartedConnecting(networManagingActor))

      // Crash again
      remoteActor ! "FailWithMe"

      // Shouldn't get any error back
      errorHandler.expectNoMsg(2.seconds)
    }
    it("can send message through the connection actor") {
      val errorHandler = ignoreConnectionMsgProbe()
      val useRealConnection = new AtomicBoolean(false)
      val socket = new MockSocket()
      val client = MobileRemoteClient.createClient(TestDevice(),
        socketFactory =
          a => {
            if (useRealConnection.get) {
              socket
            } else {
              MockSockets.ThrowOnAccess
            }
          },
        errorHandler = errorHandler.ref)

      val remoteActor = client.actorFor("not-existing", "not-existing.localhost", 1337)

      remoteActor ! "FailWithMe"
      val errorMsg = errorHandler.receiveOne(10.seconds).asInstanceOf[CommunicationError]
      useRealConnection.set(true)
      errorMsg.connectionManagingActor.!(PleaseTryToReconnect)(errorHandler.ref)
      errorMsg.connectionManagingActor.!(errorMsg.unsentMessages.head)(errorHandler.ref)

      socket.awaitWrite()

    }
  }

}
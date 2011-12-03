package akka.mobile.server

import org.scalatest.Spec
import java.net.InetSocketAddress
import akka.actor.Actor
import akka.testkit.TestProbe
import org.scalatest.matchers.ShouldMatchers
import akka.mobile.communication.NetworkFailures._
import akka.mobile.client.{SocketRepresentation, TCPSocket, MobileRemoteClient}
import akka.mobile.testutils._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.jboss.netty.channel._
import java.io.IOException
import akka.mobile.communication.Registry
import akka.mobile.server.ServerEvents.{ClientConnected, ClientDisconnected}
import akka.mobile.server.C2MDCommunication.SendMessageAndUseC2MDFallback

/**
 * @author roman.stoffel@gamlor.info
 * @since 11.11.11
 */

class FailureHandlingSpec extends Spec with ShouldMatchers {

  import akka.util.duration._

  val ECHO_ACTOR_ID = "echo"

  describe("Error on Client:") {
    it("notifies error handler actor") {
      val errorHandler = TestProbe()
      TestServer.withRunningServer(errorHandler.ref, c => {

        val client = createClientWithConnectionClosingEchoActor(c)

        client.actorFor(ECHO_ACTOR_ID, "localhost", c.port).!("Ask")(BlackholeActor())

        errorHandler.ignoreMsg({
          case StartedConnecting(_) => true
        })

        val msg = errorHandler.receiveOne(50.seconds).asInstanceOf[CommunicationError]
        msg.lastException.isInstanceOf[IOException] should be(true)
      })
    }
    it("expect a disconnect event") {
      val errorHandler = TestProbe()
      TestServer.withRunningServer(errorHandler.ref, c => {

        val client = createClientWithConnectionClosingEchoActor(c)


        val expectDisconnectMsg = TestProbe()
        expectDisconnectMsg.ignoreMsg({
          case ClientConnected(_) => true
        })
        c.server.addListener(expectDisconnectMsg.ref)
        client.actorFor(ECHO_ACTOR_ID, "localhost", c.port).!("Ask")(BlackholeActor())


        expectDisconnectMsg.expectMsgPF(5.seconds)({
          case ClientDisconnected(TestDevice.clientId) => true
        })
      })
    }
    it("get messages for failed sending back") {

      val errorHandler = TestProbe()
      TestServer.withRunningServer(errorHandler.ref, c => {
        val client = createClientWithConnectionClosingEchoActor(c)

        val remoteActor = client.actorFor(ECHO_ACTOR_ID, "localhost", c.port)

        remoteActor.!("SendMsg1")(BlackholeActor())
        remoteActor.!("SendMsg2")(BlackholeActor())
        remoteActor.!("SendMsg3")(BlackholeActor())

        errorHandler.ignoreMsg({
          case StartedConnecting(_) => true
        })

        val msgs = errorHandler.receiveOne(10.seconds).asInstanceOf[CommunicationError].unsentMessages
        msgs.size should be >= (1)
        msgs.size should be <= (3)
      })
    }
    it("can send restart has no effect") {
      val errorHandler = TestProbe()

      TestServer.withRunningServer(errorHandler.ref, c => {
        val client = createClientWithConnectionClosingEchoActor(c)

        val remoteActor = client.actorFor(ECHO_ACTOR_ID, "localhost", c.port)

        remoteActor.!("FailWithMe")(BlackholeActor())

        errorHandler.ignoreMsg({
          case StartedConnecting(_) => true
        })

        errorHandler.receiveOne(5.seconds)
          .asInstanceOf[CommunicationError].connectionManagingActor ! PleaseTryToReconnect
      })
    }
    it("can send message through the connection actor") {
      val errorHandler = TestProbe()
      val eventHandler = new ServerMessagesToClientHandler(new Registry(), errorHandler.ref, NoC2MDAvailable);

      val client = TestDevice.clientId
      val faultyChannel = mock(classOf[Channel])
      val future = new FailedChannelFuture(faultyChannel, new IOException("IO error"))
      when(faultyChannel.write(anyObject())).thenReturn(future)
      eventHandler.putChannelForClient(client, faultyChannel);

      eventHandler.send(Left(client), Left("actor-on-client"), "Test-Message", None)

      val msg = errorHandler.receiveOne(50.seconds).asInstanceOf[CommunicationError]
      msg.connectionManagingActor ! msg.unsentMessages.head

      verify(faultyChannel, times(2)).write(msg.unsentMessages.head.msg)
    }
    it("falls back to C2MD if message is marked") {
      val errorHandler = BlackholeActor()
      val c2md = new C2MDMock()
      val eventHandler = new ServerMessagesToClientHandler(new Registry(), errorHandler, c2md);

      val client = TestDevice.clientId

      val testMessage = new C2MDTestMessage()

      eventHandler.send(Left(client), Left("actor-on-client"), testMessage, None)

      c2md.hasMessageBeenSent should be(true)
    }
    it("does not fall back to C2MD if message isn't marked") {
      val errorHandler = BlackholeActor()
      val c2md = new C2MDMock()
      val eventHandler = new ServerMessagesToClientHandler(new Registry(), errorHandler, c2md);

      val client = TestDevice.clientId

      val testMessage = new RegularTestMessage()

      eventHandler.send(Left(client), Left("actor-on-client"), testMessage, None)

      c2md.hasMessageBeenSent should be(false)
    }
    it("force C2MD for a message through the connection actor") {
      val errorHandler = TestProbe()
      val c2md = new C2MDMock()
      val eventHandler = new ServerMessagesToClientHandler(new Registry(), errorHandler.ref, c2md);

      val client = TestDevice.clientId
      eventHandler.send(Left(client), Left("actor-on-client"), "Test-Message", None)

      val error = errorHandler.receiveOne(50.seconds).asInstanceOf[CommunicationError]
      val msg = error.unsentMessages.head
      error.connectionManagingActor ! SendMessageAndUseC2MDFallback(msg.msg, msg.sender)


      c2md.expectMessageBeenSent()
    }
  }
  describe("The server event handler") {
    it("notifies the error handler on error") {
      val errorHandler = TestProbe()
      val eventHandler = new ServerMessagesToClientHandler(new Registry(), errorHandler.ref, NoC2MDAvailable);

      val client = TestDevice.clientId
      val faultyChannel = mock(classOf[Channel])
      val future = new FailedChannelFuture(faultyChannel, new IOException("IO error"))
      when(faultyChannel.write(anyObject())).thenReturn(future)
      eventHandler.putChannelForClient(client, faultyChannel);

      eventHandler.send(Left(client), Left("actor-on-client"), "Test-Message", None)

      val msg = errorHandler.receiveOne(50.seconds).asInstanceOf[CommunicationError]
      msg.lastException.isInstanceOf[IOException] should be(true)
    }
    it("notifies when client reconnected") {
      val errorHandler = TestProbe()
      TestServer.withRunningServer(errorHandler.ref, c => {
        val client = createClientWithConnectionClosingEchoActor(c)

        val remoteActor = client.actorFor(ECHO_ACTOR_ID, "localhost", c.port)

        remoteActor.!("SendMsg1")(BlackholeActor())

        errorHandler.expectMsgAllClassOf(2.seconds, classOf[StartedConnecting])
      })
    }
  }


  def registerEchoActor(serverContext: TestServerContext, socket: TCPSocket) {
    serverContext.register(ECHO_ACTOR_ID, Actor.actorOf(new DisconnectOnReceiveEcho(socket)))
  }

  def createClientWithConnectionClosingEchoActor(c: TestServerContext) = {
    val clientSocket = new TCPSocket(new InetSocketAddress("localhost", c.port))
    val client = MobileRemoteClient.createClient(
      TestDevice(), errorHandler = BlackholeActor(), socketFactory = a => clientSocket)
    registerEchoActor(c, clientSocket);
    client
  }

  class DisconnectOnReceiveEcho(toClose: SocketRepresentation) extends Actor {
    protected def receive = {
      case msg => {
        toClose.close()
        self.reply(msg)
      }
    }
  }

}

case class C2MDTestMessage() extends SentThroughC2MDIfNoConnectionIsAvailable

case class RegularTestMessage()
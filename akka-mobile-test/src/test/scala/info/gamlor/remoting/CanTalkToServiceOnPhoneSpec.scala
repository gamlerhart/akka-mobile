package info.gamlor.remoting

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.actor.Actor
import akka.testkit.TestProbe
import akka.mobile.testutils.{TestServerContext, TestDevice, EchoActor, TestServer}
import akka.mobile.client.{RemoteClient}
import akka.mobile.server.ServerEvents.{ClientDisconnected, ClientConnected}

/**
 * @author roman.stoffel@gamlor.info
 * @since 19.11.11
 */

class CanTalkToServiceOnPhoneSpec extends Spec with ShouldMatchers {

  describe("From the server") {
    it("we can talk to a service on the client") {
      TestServer.withRunningServer(ctx => {
        val client = ctx.connectAClient()
        client.connectNow()

        client.register("echo", Actor.actorOf[EchoActor])

        waitUntilConnected(ctx, client)


        val actor = ctx.server.actorOf(client.clientId, "echo")

        val sender = TestProbe()
        actor.!("Hi")(sender.ref)
        sender.expectMsg("Hi")
      })
    }
    it("talk back to a server on event") {
      TestServer.withRunningServer(ctx => {
        val client = ctx.connectAClient()

        val sender = TestProbe()
        client.register("echo", sender.ref)

        ctx.server.addListener(Actor.actorOf(new Actor() {
          protected def receive = {
            case ClientConnected(clientId) => {
              ctx.server.actorOf(clientId, "echo") ! "Hi"
            }
          }
        }))
        client.connectNow()
        sender.expectMsg("Hi")
      })
    }
    it("works with without .connectNow") {
      TestServer.withRunningServer(ctx => {
        val client = ctx.connectAClient()
        client.register("echo", Actor.actorOf[EchoActor])
        val echoActorOnServer = ctx.registerStandardEchoActor()
        (client.actorFor(echoActorOnServer) ? "Hi").await

        val actor = ctx.server.actorOf(client.clientId, "echo")
        val sender = TestProbe()
        actor.!("Hi")(sender.ref)
        sender.expectMsg("Hi")
      })
    }
  }


  def waitUntilConnected(ctx: TestServerContext, client: RemoteClient) {
    val waitTillConnected = TestProbe()
    ctx.register("WaitTillConnected", waitTillConnected.ref)
    client.actorFor("WaitTillConnected") ! "WaitTillConnected"
    waitTillConnected.expectMsg("WaitTillConnected")
  }
}
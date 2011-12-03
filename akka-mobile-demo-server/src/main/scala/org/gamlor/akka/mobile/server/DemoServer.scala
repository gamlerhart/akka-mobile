package org.gamlor.akka.mobile.server

import akka.mobile.server.{InMemoryClientDatabase, NettyRemoteServer}
import akka.actor.Actor
import akka.mobile.communication.ClientId
import akka.mobile.communication.NetworkFailures.CannotSendDueNoConnection
import akka.mobile.communication.CommunicationMessages._
import akka.mobile.server.C2MDCommunication.SendMessageAndUseC2MDFallback

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.11.11
 */

object PlayWithAkka extends App {
  main()

  def main() {
    val chatServer = NettyRemoteServer.start("0.0.0.0", 2552,
      database = Some(new InMemoryClientDatabase(new H2Database("jdbc:h2:~/apiKeyStore"))),
      errorHandler = Actor.actorOf(new Actor() {
        protected def receive = {
          case CannotSendDueNoConnection(e, m, mngr) => {
            mngr ! SendMessageAndUseC2MDFallback(m.msg, m.sender)
          }
          case m => {
            println(m)
            //mngr ! SendMessageAndUseC2MDFallback(m.msg, m.sender)
          }
        }
      }).start());



    chatServer.register("chat-service", Actor.actorOf[ChatService])

    val serviceOnPhone = chatServer.actorOf(ClientId("a807077fc3090684", "info.gamlor.akkamobile"), "notifications")
    serviceOnPhone ! "Hi"
  }
}

class ChatService extends Actor {
  protected def receive = {
    case SendMessageToChat(msg) => {
      self.reply(AddMessageToChat(msg))
    }
  }
}


@SerialVersionUID(1L)
case class SendMessageToChat(msg: String) {

}

@SerialVersionUID(1L)
case class AddMessageToChat(msg: String) {

}
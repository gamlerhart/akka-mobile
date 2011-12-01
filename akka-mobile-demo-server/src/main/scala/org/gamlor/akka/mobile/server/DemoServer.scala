package org.gamlor.akka.mobile.server

import akka.mobile.server.{InMemoryClientDatabase, NettyRemoteServer}
import akka.actor.Actor

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.11.11
 */

object PlayWithAkka extends App {
  main()

  def main() {
    val chatServer = NettyRemoteServer.start("0.0.0.0", 2552,
      database = Some(new InMemoryClientDatabase(new H2Database("jdbc:h2:~/apiKeyStore"))));

    chatServer.register("chat-service", Actor.actorOf[ChatService])
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
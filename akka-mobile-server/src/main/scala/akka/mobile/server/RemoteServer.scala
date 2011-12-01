package akka.mobile.server

import akka.actor.ActorRef
import akka.util.ListenerManagement
import akka.mobile.communication.ClientId
import java.lang.String

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait RemoteServer extends ListenerManagement {
  def connectionStateOf(clientId: ClientId): ClientConnectionState

  def actorOf(clientId: ClientId, id: String): ActorRef

  def register(idOfActor: String, actorRef: ActorRef)


  def shutdownServerModule()
}



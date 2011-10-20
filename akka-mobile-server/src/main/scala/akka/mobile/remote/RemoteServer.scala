package akka.mobile.remote

import akka.actor.ActorRef

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait RemoteServer {
  def register(idOfActor: String, actorRef: ActorRef)


  def shutdownServerModule()
}



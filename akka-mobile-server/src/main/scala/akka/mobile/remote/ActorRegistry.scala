package akka.mobile.remote

import java.util.concurrent.ConcurrentHashMap
import java.lang.IllegalStateException
import akka.actor.{Actor, ActorRef}

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

class ActorRegistry {
  private val actors = new ConcurrentHashMap[String, ActorRef]()
  def findActorById(id:String) : ActorRef = {
    actors.get(id) match{
      case null => throw new IllegalArgumentException("Couldn't find actor for id "+id)
      case actor => actor
    }
  }
  def registerActor(id:String,actor:ActorRef) {
    val existingActor = actors.putIfAbsent(id,actor)
    if(existingActor != null){
      throw new IllegalStateException("Cannot register multiple actors for the id "+id)
    }
  }
}
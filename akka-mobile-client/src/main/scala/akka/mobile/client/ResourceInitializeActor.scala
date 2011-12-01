package akka.mobile.client

import akka.actor.Actor

/**
 * Problem: Multiple actors need to access the same, shared classic resource (like a socket, db-connection).
 * The initialisation can fail and so can do operations on this resource. On failure all actors which
 * use this resource should be restarted and the resource disposed and reallocated. How do you solve this?
 *
 * With this actor & the AllForOneStrategy. This actor just initializes the resource and return it to other actors.
 * In case a failure happen all actors are restarted, including this actor. That means the resource is
 * also allocated only once per failure / restart cicle for all actors in that group
 *
 * Any message will return the resource. Alternatively you can use the GetResource message
 * @author roman.stoffel@gamlor.info
 * @since 29.10.11
 */
class ResourceInitializeActor[TResource](factory: () => TResource,
                                         closeAction: Option[TResource => Unit]) extends Actor {
  private var cachedValue: Option[TResource] = None

  override def preStart() {
    cachedValue = None
  }

  protected def receive = {
    case _ => {
      if (!cachedValue.isDefined) {
        cachedValue = Some(factory())
      }
      self.reply(cachedValue.get)
    }
  }


  override def preRestart(reason: Throwable, message: Option[Any]) {
    closeResource()
  }

  override def postStop() {
    closeResource()
  }

  def closeResource() {
    cachedValue.foreach(value => {
      closeAction.foreach(ca => ca(value))
    })
  }
}

object ResourceInitializeActor {
  def apply[TResource](factory: () => TResource): ResourceInitializeActor[TResource]
  = new ResourceInitializeActor(factory, None)

  def apply[TResource](factory: () => TResource,
                       closeAction: TResource => Unit): ResourceInitializeActor[TResource]
  = new ResourceInitializeActor(factory, Some(closeAction))

  case object GetResource

}
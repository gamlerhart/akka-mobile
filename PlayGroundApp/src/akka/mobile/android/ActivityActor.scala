package akka.mobile.android

import akka.actor.{UntypedChannel, ActorRef, Actor, ForwardableChannel}


/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

trait ActivityActor extends ForwardableChannel {

  final def self: ActorRef = {
    innerDispatchingActor
  }

  protected def receive: Actor.Receive

  val innerDispatchingActor = Actor.actorOf(new ActorProxy(this)).start()

  implicit def sender: ActorRef = {
    (innerDispatchingActor)
  }


  def channel = {
    throw new Error("Hmm")
  }


  def !(msg: Any)(implicit sender: UntypedChannel) {
    innerDispatchingActor.!(msg)(sender)
  }

  class ActorProxy(outer: ActivityActor) extends Actor() {
    self.dispatcher = RunOnActivitiyDispatcher()

    protected def receive = outer.receive
  }

}
package akka.mobile.communication

import akka.actor._
import akka.dispatch.{MessageInvocation, MessageDispatcher}
import java.util.concurrent.atomic.AtomicReference

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

trait NotImplementedActorRef extends ActorRef with ScalaActorRef {
  // NOT SUPPORTED
  def actorClass: Class[_ <: Actor] = unsupported

  def dispatcher_=(md: MessageDispatcher): Unit = unsupported

  def dispatcher: MessageDispatcher = unsupported

  def link(actorRef: ActorRef): Unit = unsupported

  def unlink(actorRef: ActorRef): Unit = unsupported

  def startLink(actorRef: ActorRef): Unit = unsupported

  def spawn(clazz: Class[_ <: Actor]): ActorRef = unsupported

  def spawnRemote(clazz: Class[_ <: Actor], hostname: String, port: Int, timeout: Long): ActorRef = unsupported

  def spawnLink(clazz: Class[_ <: Actor]): ActorRef = unsupported

  def spawnLinkRemote(clazz: Class[_ <: Actor], hostname: String, port: Int, timeout: Long): ActorRef = unsupported

  def supervisor: Option[ActorRef] = unsupported

  def mailbox: AnyRef = unsupported

  def mailbox_=(value: AnyRef): AnyRef = unsupported

  def handleTrapExit(dead: ActorRef, reason: Throwable): Unit = unsupported

  def restart(reason: Throwable, maxNrOfRetries: Option[Int], withinTimeRange: Option[Int]): Unit = unsupported

  def restartLinkedActors(reason: Throwable, maxNrOfRetries: Option[Int], withinTimeRange: Option[Int]): Unit = unsupported

  def invoke(messageHandle: MessageInvocation): Unit = unsupported

  def supervisor_=(sup: Option[ActorRef]): Unit = unsupported

  def actorInstance: AtomicReference[Actor] = unsupported

  def registerSupervisorAsRemoteActor = unsupported

  def unsupported = throw new UnsupportedOperationException("Not supported for " + this.getClass)

}
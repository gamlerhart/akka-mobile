package akka.mobile.remote

import java.util.Map
import akka.actor._
import akka.dispatch.{MessageInvocation, MessageDispatcher}
import java.util.concurrent.atomic.AtomicReference
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

case class ClientRemoteActorRef(address : InetSocketAddress, serviceId : String) extends ActorRef with ScalaActorRef {

  id = serviceId

  @scala.deprecated("Remoting will become fully transparent in the future")
  def homeAddress = Some(address)

  @scala.deprecated("Will be removed without replacement, doesn't make any sense to have in the face of `become` and `unbecome`")
  def actorClassName = notImplemented

  def start: this.type = synchronized[this.type] {
    _status = ActorRefInternals.RUNNING
    this
  }

  def stop() {notImplemented}

  @scala.deprecated("Will be removed after 1.1, use Actor.actorOf instead")
  def spawn(clazz: Class[_]) = notImplemented

  @scala.deprecated("Will be removed after 1.1, client managed actors will be removed")
  def spawnRemote(clazz: Class[_], hostname: String, port: Int, timeout: Long) = notImplemented

  @scala.deprecated("Will be removed after 1.1, use use Actor.remote.actorOf instead and then link on success")
  def spawnLink(clazz: Class[_]) = notImplemented

  @scala.deprecated("Will be removed after 1.1, client managed actors will be removed")
  def spawnLinkRemote(clazz: Class[_], hostname: String, port: Int, timeout: Long) = notImplemented

  def postMessageToMailbox(message: Any, channel: UntypedChannel) = {
    val chSender = channel match {
      case ref: ActorRef ⇒ Some(ref)
      case _             ⇒ None
    }
    Actor.remote.send[Any](message, chSender, None, homeAddress.get, timeout, true, this, None, ActorType.ScalaActor, None)
  }

  def postMessageToMailboxAndCreateFutureResultWithTimeout(message: Any, timeout: Long, channel: UntypedChannel)  = notImplemented

  def registerSupervisorAsRemoteActor  = notImplemented

  def notImplemented = {throw new Error("Not Implemented")}

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

  def linkedActors: Map[Uuid, ActorRef] = unsupported

  def mailbox: AnyRef = unsupported

  def mailbox_=(value: AnyRef): AnyRef = unsupported

  def handleTrapExit(dead: ActorRef, reason: Throwable): Unit = unsupported

  def restart(reason: Throwable, maxNrOfRetries: Option[Int], withinTimeRange: Option[Int]): Unit = unsupported

  def restartLinkedActors(reason: Throwable, maxNrOfRetries: Option[Int], withinTimeRange: Option[Int]): Unit = unsupported

  def invoke(messageHandle: MessageInvocation): Unit = unsupported

  def supervisor_=(sup: Option[ActorRef]): Unit = unsupported

  def actorInstance: AtomicReference[Actor] = unsupported

  private def unsupported = throw new UnsupportedOperationException("Not supported for ClientRemoteActorRef")

}
package akka.mobile.server

import java.util.Map
import akka.actor._
import akka.dispatch.{DefaultCompletableFuture, ActorCompletableFuture}
import akka.mobile.communication.{MessageSink, ClientId, NotImplementedActorRef}
import com.eaio.uuid.UUID

/**
 * Represents reference to an actor on a remote device.
 * Expect extraordinary slow communication, high latency and communication failures
 * when communication with a remote device. Keep the communication to a minumum.
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

case class RemoteDeviceActorRef(clientId: ClientId,
                                remoteActorId: Either[String, UUID],
                                remoteService: MessageSink)
  extends ActorRef with ScalaActorRef with NotImplementedActorRef {

  start()

  id = clientId.toString + ":" + uuid

  @scala.deprecated("Remoting will become fully transparent in the future")
  def homeAddress = unsupported

  @scala.deprecated("Will be removed without replacement, doesn't make any sense to have in the face of `become` and `unbecome`")
  def actorClassName = unsupported


  def postMessageToMailbox(message: Any, channel: UntypedChannel) = {
    val chSender = channel match {
      case ref: ActorRef ⇒ Some(ref)
      case _ ⇒ None
    }
    remoteService.send(Left(clientId), remoteActorId, message, chSender)
  }

  def postMessageToMailboxAndCreateFutureResultWithTimeout(message: Any,
                                                           timeout: Long, channel: UntypedChannel): ActorCompletableFuture = {
    val chSender = channel match {
      case ref: ActorRef ⇒ Some(ref)
      case _ => None
    }
    val chFuture = channel match {
      case f: ActorCompletableFuture => f
      case _ => new DefaultCompletableFuture[Any](timeout)
    }
    val future = remoteService.ask(Left(clientId), remoteActorId, message, chSender, chFuture)
    ActorCompletableFuture(future)
  }

  def start(): this.type = synchronized[this.type] {
    _status = ActorRefInternals.RUNNING
    this
  }

  def stop() {
    notImplemented
  }

  def linkedActors: Map[Uuid, ActorRef]
  = throw new UnsupportedOperationException("Not supported for " + this.getClass)

  def notImplemented = {
    throw new Error("Not Implemented")
  }
}
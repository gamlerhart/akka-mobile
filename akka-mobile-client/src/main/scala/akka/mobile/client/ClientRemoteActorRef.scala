package akka.mobile.client

import java.util.Map
import akka.actor._
import java.net.InetSocketAddress
import akka.dispatch.{DefaultCompletableFuture, ActorCompletableFuture}
import akka.mobile.communication.{NotImplementedActorRef, MessageSink}
import com.eaio.uuid.UUID

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

case class ClientRemoteActorRef(address: InetSocketAddress,
                                serviceId: Either[String, UUID],
                                messageSender: MessageSink)
  extends ActorRef with ScalaActorRef with NotImplementedActorRef {

  id = serviceId.toString

  @scala.deprecated("Remoting will become fully transparent in the future")
  def homeAddress = Some(address)


  start()

  @scala.deprecated("Will be removed without replacement, doesn't make any sense to have in the face of `become` and `unbecome`")
  def actorClassName = notImplemented

  def start(): this.type = synchronized[this.type] {
    _status = ActorRefInternals.RUNNING
    this
  }

  def stop() {
    notImplemented
  }


  def postMessageToMailbox(message: Any, channel: UntypedChannel) = {
    val chSender = channel match {
      case ref: ActorRef ⇒ Some(ref)
      case _ ⇒ None
    }
    messageSender.send(Right(homeAddress.get), serviceId, message, chSender)
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
    val future = messageSender.ask(Right(homeAddress.get), serviceId, message, chSender, chFuture)
    ActorCompletableFuture(future)
  }


  def notImplemented = {
    throw new Error("Not Implemented")
  }

  override def linkedActors: Map[Uuid, ActorRef]
  = throw new UnsupportedOperationException("Not supported for " + this.getClass)


}
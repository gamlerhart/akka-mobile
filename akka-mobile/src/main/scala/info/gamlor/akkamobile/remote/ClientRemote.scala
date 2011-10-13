package info.gamlor.akkamobile.remote

import akka.dispatch.CompletableFuture
import java.net.InetSocketAddress
import akka.remoteinterface.{RemoteClientModule, RemoteSupport}
import akka.actor._

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

class ClientRemote extends RemoteSupport with RemoteClientModule{
  def registerTypedActor(id: String, typedActor: AnyRef) {}

  def isRunning = notImplemented

  def name = notImplemented

  def address =notImplemented

  def start(host: String, port: Int, loader: Option[ClassLoader])  =notImplemented

  def shutdownServerModule()  =notImplemented

  def registerTypedPerSessionActor(id: String, factory: => AnyRef)  =notImplemented

  def registerByUuid(actorRef: ActorRef)  =notImplemented

  def register(id: String, actorRef: ActorRef)  =notImplemented

  def registerPerSession(id: String, factory: => ActorRef)  =notImplemented

  def unregister(actorRef: ActorRef)  =notImplemented

  def unregister(id: String) =notImplemented

  def unregisterPerSession(id: String) =notImplemented

  def unregisterTypedActor(id: String) =notImplemented

  def unregisterTypedPerSessionActor(id: String) =notImplemented

  @scala.deprecated("Will be removed after 1.1")
  def clientManagedActorOf(factory: () => Actor, host: String, port: Int) = notImplemented

  def shutdownClientModule() =notImplemented

  def shutdownClientConnection(address: InetSocketAddress) = notImplemented

  def restartClientConnection(address: InetSocketAddress) = notImplemented

  def typedActorFor[T](intfClass: Class[T], serviceId: String, implClassName: String, timeout: Long, host: String, port: Int, loader: Option[ClassLoader]) = notImplemented

  def actorFor(serviceId: String, className: String, timeout: Long, hostname: String, port: Int, loader: Option[ClassLoader]) = notImplemented

  def send[T](message: Any, senderOption: Option[ActorRef], senderFuture: Option[CompletableFuture[T]], remoteAddress: InetSocketAddress, timeout: Long, isOneWay: Boolean, actorRef: ActorRef, typedActorInfo: Option[(String, String)], actorType: ActorType, loader: Option[ClassLoader]) = notImplemented

  def optimizeLocalScoped_?() = notImplemented

  def registerSupervisorForActor(actorRef: ActorRef): ActorRef= notImplemented

  def deregisterSupervisorForActor(actorRef: ActorRef): ActorRef = notImplemented

  @deprecated("Will be removed after 1.1", "1.1")
  def registerClientManagedActor(hostname: String, port: Int, uuid: Uuid): Unit = notImplemented

  @deprecated("Will be removed after 1.1", "1.1")
  def unregisterClientManagedActor(hostname: String, port: Int, uuid: Uuid): Unit = notImplemented

  def notImplemented = {throw new Error("Not yet implemented")}
}
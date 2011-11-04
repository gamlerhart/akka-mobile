package akka.mobile.remote

import akka.actor._
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */


trait RemoteClient {
  def actorFor(serviceId: String, hostname: String, port: Int): ActorRef

}

object MobileRemoteClient {

  /**
   * Creates a new instance.
   */
  def createClient(device: DeviceOperations) = new MobileRemoteClient(device.clientId)
}

class MobileRemoteClient(clientId: ClientId) extends RemoteClient {

  val remoteMessaging = RemoteMessaging(clientId)


  def actorFor(serviceId: String, hostname: String, port: Int) = {
    ClientRemoteActorRef(new InetSocketAddress(hostname, port), serviceId, remoteMessaging.msgSink)
  }

  def actorFor(serviceId: String, className: String, timeout: Long,
               hostname: String, port: Int, loader: Option[ClassLoader]) = {
    ClientRemoteActorRef(new InetSocketAddress(hostname, port), serviceId, remoteMessaging.msgSink)
  }

}




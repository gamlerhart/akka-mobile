package akka.mobile.remote

import akka.actor._
import java.net.InetSocketAddress
import java.lang.IllegalStateException

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */


trait RemoteClient {
  def actorFor(serviceId: String, hostname: String, port: Int): ActorRef

  def actorFor(serviceId: String): ActorRef

}

object MobileRemoteClient {

  /**
   * Creates a new instance.
   */
  def createClient(device: DeviceOperations) = new MobileRemoteClient(device.clientId, None, None)

  /**
   * Creates a new instance.
   */
  def createClient(device: DeviceOperations, hostname: String, port: Int) = new MobileRemoteClient(device.clientId, None, None)
}

class MobileRemoteClient(clientId: ClientId, hostname: Option[String], port: Option[Int]) extends RemoteClient {

  val remoteMessaging = RemoteMessaging(clientId)


  def actorFor(serviceId: String, hostname: String, port: Int) = {
    ClientRemoteActorRef(new InetSocketAddress(hostname, port), serviceId, remoteMessaging.msgSink)
  }

  def actorFor(serviceId: String) = {
    if (hostname.isEmpty || port.isEmpty) {
      throw new IllegalStateException("You need to specify the hostname and port when creating the remote support " +
        "in order to use this method. You can do this when creating the client instance or in the configuration")
    }
    actorFor(serviceId, hostname.get, port.get)
  }
}




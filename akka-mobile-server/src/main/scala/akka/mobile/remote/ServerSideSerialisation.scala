package akka.mobile.remote

import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol._
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */
case class ServerInfo(hostName: String, port: Int)

class ServerSideSerialisation(msgSink: MessageSink, serverInfo: ServerInfo) extends Serialisation {

  def toAddressProtocol(actorRef: ActorRef) = {
    AddressProtocol.newBuilder
      .setType(AddressType.SERVICE_ADDRESS)
      .build()

  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol, clientID: Either[ClientId, InetSocketAddress]) = {
    val remoteActorId = refInfo.getClassOrServiceName
    val homeAddress = refInfo.getNodeAddress
    homeAddress.getType match {
      case AddressType.DEVICE_ADDRESS => {
        RemoteDeviceActorRef(deserializeClientId(homeAddress.getDeviceAddress),
          remoteActorId, msgSink)
      }
      case AddressType.SERVICE_ADDRESS => throw new Error("TODO")
      case ue => throw new Error("Unexpected type " + ue)
    }
  }


  def deserializeClientId(deviceAddress: DeviceAddress)
  = ClientId(deviceAddress.getDeviceID, deviceAddress.getAppId)

}
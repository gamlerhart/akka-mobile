package akka.mobile.remote

import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol._
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

class ClientSideSerialisation(messageSender: MessageSink, deviceAddress: ClientId) extends Serialisation {
  def toAddressProtocol() = {
    AddressProtocol.newBuilder
      .setType(AddressType.DEVICE_ADDRESS)
      .setDeviceAddress(DeviceAddress.newBuilder().setAppId(deviceAddress.applicationId).setDeviceID(deviceAddress.clientId))
      .build
  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol, nodeAddress: AddressProtocol, clientId: Either[ClientId, InetSocketAddress]): ActorRef = {
    val remoteActorId = refInfo.getClassOrServiceName
    val serverID = clientId.right.get
    nodeAddress.getType match {
      case AddressType.SERVICE_ADDRESS => {
        ClientRemoteActorRef(
          new InetSocketAddress(serverID.getHostName, serverID.getPort),
          remoteActorId, messageSender);
      }
      case AddressType.DEVICE_ADDRESS
      => throw new IllegalArgumentException("Cannot send answers back to other mobile devices")
      case ue => throw new Error("Unexpected type " + ue)
    }
  }
}
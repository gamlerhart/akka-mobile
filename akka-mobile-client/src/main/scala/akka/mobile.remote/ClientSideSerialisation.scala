package akka.mobile.remote

import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol._
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

class ClientSideSerialisation(messageSender: MessageSink, deviceAddress: ClientId) extends Serialisation {
  def toAddressProtocol(actorRef: ActorRef) = {
    AddressProtocol.newBuilder
      .setType(AddressType.DEVICE_ADDRESS)
      .setDeviceAddress(DeviceAddress.newBuilder().setAppId(deviceAddress.applicationId).setDeviceID(deviceAddress.clientId))
      .build
  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol, ctxInfo: InetSocketAddress): ActorRef = {
    val remoteActorId = refInfo.getClassOrServiceName
    val homeAddress = refInfo.getNodeAddress
    homeAddress.getType match {
      case AddressType.SERVICE_ADDRESS => {
        ClientRemoteActorRef(
          new InetSocketAddress(ctxInfo.getHostName, ctxInfo.getPort),
          remoteActorId, messageSender);
      }
      case AddressType.DEVICE_ADDRESS
      => throw new IllegalArgumentException("Cannot send answers back to other mobile devices")
      case ue => throw new Error("Unexpected type " + ue)
    }
  }
}
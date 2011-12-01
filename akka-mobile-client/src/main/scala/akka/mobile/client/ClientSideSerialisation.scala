package akka.mobile.client

import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol._
import java.net.InetSocketAddress
import akka.mobile.communication.{Serialisation, MessageSink, ClientId}
import java.lang.UnsupportedOperationException

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

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol,
                          nodeId: Either[ClientId, InetSocketAddress]): ActorRef = {
    val remoteActorId = deSerializeActorRef(refInfo)
    nodeId match {
      case Right(address) => {
        ClientRemoteActorRef(
          new InetSocketAddress(address.getHostName, address.getPort),
          remoteActorId, messageSender)
      }
      case Left(_) => throw new UnsupportedOperationException("The client cannot deal with other client actor references: " + nodeId)
    }
  }
}
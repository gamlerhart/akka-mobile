package akka.mobile.server

import akka.mobile.protocol.MobileProtocol._
import java.net.InetSocketAddress
import akka.mobile.communication.{ClientId, MessageSink, Serialisation}
import java.lang.UnsupportedOperationException

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */
case class ServerInfo(hostName: String, port: Int)

class ServerSideSerialisation(msgSink: MessageSink) extends Serialisation {

  def toAddressProtocol() = {
    AddressProtocol.newBuilder
      .setType(AddressType.SERVICE_ADDRESS)
      .build()

  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol,
                          nodeId: Either[ClientId, InetSocketAddress]) = {
    nodeId match {
      case Left(clientId) => RemoteDeviceActorRef(clientId, deSerializeActorRef(refInfo), msgSink)
      case Right(_) => {
        throw new UnsupportedOperationException("Expect a client id, but got " + nodeId)
      }
    }
  }


  def deserializeClientId(deviceAddress: DeviceAddress)
  = ClientId(deviceAddress.getDeviceID, deviceAddress.getAppId)

}
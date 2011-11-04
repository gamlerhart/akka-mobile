package akka.mobile.testutils

import akka.mobile.protocol.MobileProtocol.{DeviceAddress, AddressType, AddressProtocol, RemoteActorRefProtocol}
import java.net.InetSocketAddress
import akka.mobile.remote.{ClientId, Serialisation}

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

object MockSerialisation extends Serialisation {
  def toAddressProtocol() = mockAddress()

  def mockAddress() = {
    AddressProtocol.newBuilder()
      .setType(AddressType.DEVICE_ADDRESS)
      .setDeviceAddress(DeviceAddress.newBuilder().setAppId("mock").setDeviceID("device-mock"))
      .build()

  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol, nodeAddress: AddressProtocol, clientID: Either[ClientId, InetSocketAddress]) = {
    null
  }
}
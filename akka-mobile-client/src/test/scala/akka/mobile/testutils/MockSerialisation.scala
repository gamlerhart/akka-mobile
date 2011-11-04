package akka.mobile.testutils

import akka.mobile.remote.Serialisation
import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol.{DeviceAddress, AddressType, AddressProtocol, RemoteActorRefProtocol}
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

object MockSerialisation extends Serialisation {
  def toAddressProtocol(actorRef: ActorRef) = mockAddress()

  def mockAddress() = {
    AddressProtocol.newBuilder()
      .setType(AddressType.DEVICE_ADDRESS)
      .setDeviceAddress(DeviceAddress.newBuilder().setAppId("mock").setDeviceID("device-mock"))
      .build()

  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol, ctxInfo: InetSocketAddress) = {
    null
  }
}
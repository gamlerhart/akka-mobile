package akka.mobile.remote

import akka.remote.protocol.RemoteProtocol.RemoteMessageProtocol
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

class RemoteMessaging {

  def channelFor(address : InetSocketAddress) : RemoteMessageChannel = null


}


class RemoteMessageChannel{
  def send(msg:RemoteMessageProtocol)  = null
}


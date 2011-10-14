package akka.mobile.remote

import akka.remote.protocol.RemoteProtocol.RemoteMessageProtocol
import java.net.{Socket, InetSocketAddress}
import java.io.{OutputStream, InputStream}

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

class RemoteMessaging(socketFactory : InetSocketAddress =>SocketRepresentation) {

  def channelFor(address: InetSocketAddress): RemoteMessageChannel
    = RemoteMessageChannel(address,socketFactory)


}

object RemoteMessaging {
  val DEFAULT_TCP_SOCKET_FACTOR = (address:InetSocketAddress) => new TCPSocket(address)
  def apply() = new RemoteMessaging(DEFAULT_TCP_SOCKET_FACTOR)
  def apply(socketFactory : InetSocketAddress =>SocketRepresentation ) = new RemoteMessaging(socketFactory)
}


case class RemoteMessageChannel(address: InetSocketAddress, socketFactory : InetSocketAddress=>SocketRepresentation) {
  val socket = socketFactory(address)
  def send(msg: RemoteMessageProtocol)  {
    msg.writeDelimitedTo(socket.out)
    socket.out.flush()
  }
}

trait SocketRepresentation{

  def in:InputStream
  def out:OutputStream

  def close()
}

class TCPSocket(addr: InetSocketAddress) extends  SocketRepresentation{
  val socket = {
    val s = new Socket()
    s.connect(addr)
    s.setKeepAlive(true)
    s
  }

  val out = socket.getOutputStream
  val in = socket.getInputStream

  def close() = socket.close()
}


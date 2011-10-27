package akka.mobile.remote

import java.io.{OutputStream, ByteArrayOutputStream, ByteArrayInputStream}

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

class MockSocket() extends SocketRepresentation {
  val outBuffer = new ByteArrayOutputStream()

  def in = new ByteArrayInputStream(outBuffer.toByteArray)

  def out: OutputStream = outBuffer

  def close() {
    outBuffer.close()
  }

  def asInputStreamFrom(startLocation: Int) = new ByteArrayInputStream(outBuffer.toByteArray.drop(startLocation))
}





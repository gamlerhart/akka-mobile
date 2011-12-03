package akka.mobile.client

import java.io._
import akka.util.ListenerManagement
import java.util.concurrent.CountDownLatch


/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

class MockSocket() extends SocketRepresentation {

  private val hasWrittenSomething = new CountDownLatch(1)
  private val hasBeenClosed = new CountDownLatch(1)
  val outBuffer = new ByteArrayOutputStream() {
    override def write(b: Int) {
      super.write(b)
      hasWrittenSomething.countDown()
    }

    override def write(b: Array[Byte], off: Int, len: Int) {
      super.write(b, off, len)
      hasWrittenSomething.countDown()
    }

    override def writeTo(out: OutputStream) {
      super.writeTo(out)
      hasWrittenSomething.countDown()
    }
  }
  @volatile var closed: Boolean = false

  def in = {
    throwOnClosed()
    new ByteArrayInputStream(outBuffer.toByteArray)
  }

  def out: OutputStream = {
    throwOnClosed()
    outBuffer
  }

  def close() {
    closed = true
    hasBeenClosed.countDown()
    outBuffer.close()
  }

  def awaitWrite() {
    hasWrittenSomething.await()
  }

  def awaitClose() {
    hasBeenClosed.await()
  }

  private def throwOnClosed() {
    if (closed) {
      throw new IOException("MockSocket is closed")
    }
  }

  def asInputStreamFrom(startLocation: Int) = new ByteArrayInputStream(outBuffer.toByteArray.drop(startLocation))
}

object MockSockets {
  val ThrowOnAccess: SocketRepresentation = new SocketRepresentation {
    def in = throw new IOException("ThrowOnAccess-implementaiton")

    def out = throw new IOException("ThrowOnAccess-implementaiton")

    def close() = {}
  }
}





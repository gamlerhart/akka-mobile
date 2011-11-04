package akka.mobile.remote

import akka.actor.UntypedChannel._
import akka.mobile.protocol.MobileProtocol.MobileMessageProtocol
import java.net.InetSocketAddress
import akka.dispatch.{Future, ActorCompletableFuture}

/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */

class WireMessageDispatcher(private val actorRegistry: Registry,
                            messageSink: MessageSink,
                            serialisation: Serialisation) {

  /**
   * We pass the sender ref cause deserializing that one is done on
   * the actual server/client part. Because that reference is different depending
   * if we're on the server or client device. And we don't want to have the code
   * for server-side references on the client.
   */
  def dispatchToActor(requestMessage: MobileMessageProtocol, ctxInfo: InetSocketAddress) {
    val actorInfo = requestMessage.getActorInfo
    val actor = actorRegistry.findActorById(actorInfo.getId)

    val (msgForActor, sender) = serialisation.deSerializeMsg(requestMessage, ctxInfo);

    if (requestMessage.getOneWay) {
      actor.postMessageToMailbox(msgForActor, sender)
    } else {
      val future = new ActorCompletableFuture(actor.timeout)
      actor.postMessageToMailboxAndCreateFutureResultWithTimeout(msgForActor,
        actor.timeout, future
      )
      future.onComplete(f => handleAnswer(f, requestMessage))
    }
  }

  def handleAnswer(future: Future[Any], message: MobileMessageProtocol) {
    future.value.get match {
      case l: Left[Throwable, Any] => throw new Error("Not yet implemented")
      case r: Right[Throwable, Any] => {
        val uuid = serialisation.deSerializeUUID(message.getUuid)
        messageSink.sendResponse(uuid, r)
      }
    }
  }

}
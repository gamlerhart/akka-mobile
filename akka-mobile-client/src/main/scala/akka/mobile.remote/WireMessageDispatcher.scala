package akka.mobile.remote

import akka.actor.UntypedChannel._
import akka.mobile.protocol.MobileProtocol.MobileMessageProtocol
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */

class WireMessageDispatcher(private val actorRegistry: Registry, serialisation: Serialisation) {

  /**
   * We pass the sender ref cause deserializing that one is done on
   * the actual server/client part. Because that reference is different depending
   * if we're on the server or client device. And we don't want to have the code
   * for server-side references on the client.
   */
  def dispatchToActor(message: MobileMessageProtocol, ctxInfo: InetSocketAddress) {
    val actorInfo = message.getActorInfo
    val actor = actorRegistry.findActorById(actorInfo.getId)

    val (msgForActor, sender) = serialisation.deSerializeMsg(message, ctxInfo);

    if (message.getOneWay) {
      actor.postMessageToMailbox(msgForActor, sender)
    } else {
      throw new Error("Not yet implemented")
    }
  }

}
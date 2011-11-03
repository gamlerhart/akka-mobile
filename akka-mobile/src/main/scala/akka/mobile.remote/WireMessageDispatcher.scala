package akka.mobile.remote

import akka.actor.UntypedChannel._
import akka.mobile.protocol.MobileProtocol.MobileMessageProtocol
import akka.actor.ActorRef

/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */

class WireMessageDispatcher(private val actorRegistry: Registry) {

  /**
   * We pass the sender ref cause deserializing that one is done on
   * the actual server/client part. Because that reference is different depending
   * if we're on the server or client device. And we don't want to have the code
   * for server-side references on the client.
   */
  def dispatchToActor(message: MobileMessageProtocol, sender: Option[ActorRef]) {
    val actorInfo = message.getActorInfo
    val actor = actorRegistry.findActorById(actorInfo.getId)

    val msgForActor = Serialisation.deSerializeMsg(message.getMessage);

    if (message.getOneWay) {
      actor.postMessageToMailbox(msgForActor, sender)
    } else {
      throw new Error("Not yet implemented")
    }
  }

}
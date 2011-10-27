package akka.mobile.remote

import akka.mobile.protocol.MobileProtocol.MobileMessageProtocol
import akka.actor.ActorRef
import akka.actor.UntypedChannel._

/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */

class WireMessageDispatcher(private val actorRegistry: ActorRegistry) {

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
package akka.mobile.communication

import akka.actor.UntypedChannel._
import java.net.InetSocketAddress
import akka.dispatch.{Future, ActorCompletableFuture}
import akka.mobile.protocol.MobileProtocol.{RemoteActorRefProtocol, MobileMessageProtocol}
import java.lang.{IllegalArgumentException, String}
import akka.actor.{ActorRef, Actor}

/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */

class WireMessageDispatcher(futures: FutureResultHandling,
                            registry: Registry,
                            messageSink: MessageSink,
                            serialisation: Serialisation) {


  def dispatchMessage(requestMessage: MobileMessageProtocol,
                      nodeId: Either[ClientId, InetSocketAddress]) {
    dispatchMessage(requestMessage, Some(nodeId))
  }

  def dispatchMessage(requestMessage: MobileMessageProtocol,
                      nodeId: Option[Either[ClientId, InetSocketAddress]]) {
    val isAnswer = requestMessage.hasAnswerFor
    val (msgForActor, sender) = serialisation.deSerializeMsg(requestMessage, nodeId);

    if (isAnswer) {
      val answer = msgForActor.asInstanceOf[Either[Exception, Any]]
      val responseID = serialisation.deSerializeUUID(requestMessage.getAnswerFor)
      answer match {
        case Left(e) => {
          futures.completeWithError(responseID, e)
        }
        case Right(v) => {
          futures.completeWithResult(responseID, v)
        }
      }
    }
    else {
      val actorInfo = requestMessage.getActorInfo
      val actor = actorFor(actorInfo.getTarget)

      if (requestMessage.getOneWay) {
        actor.postMessageToMailbox(msgForActor, sender)
      } else {
        val future = new ActorCompletableFuture(actor.timeout)
        actor.postMessageToMailboxAndCreateFutureResultWithTimeout(msgForActor,
          actor.timeout, future
        )
        future.onComplete(f => {
          handleAnswer(f, requestMessage, nodeId.get)
        })
      }
    }
  }

  def lookupByName(serviceName: String): ActorRef = {
    registry.findActorById(serviceName)
  }


  def actorFor(protocol: RemoteActorRefProtocol): ActorRef = {
    if (protocol.hasUuid) {
      val uuid = serialisation.deSerializeUUID(protocol.getUuid)
      Actor.registry.actorFor(serialisation.deSerializeUUID(protocol.getUuid))
        .getOrElse(throw new IllegalArgumentException("Couldn't find actor with uuid " + uuid + ". Maybe the actor has been stopped"))
    } else {
      lookupByName(protocol.getServiceName)
    }
  }

  def handleAnswer(future: Future[Any], message: MobileMessageProtocol,
                   nodeId: Either[ClientId, InetSocketAddress]) {
    future.value.get match {
      case l: Left[Throwable, Any] => throw new Error("Not yet implemented")
      case r: Right[Throwable, Any] => {
        val uuid = serialisation.deSerializeUUID(message.getUuid)
        messageSink.sendResponse(nodeId, uuid, r)
      }
    }
  }

}
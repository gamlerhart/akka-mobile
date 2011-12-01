package akka.mobile.communication

/**
 * @author roman.stoffel@gamlor.info
 * @since 19.11.11
 */

object InternalActors {
  /**
   * In case a client wants to eagerly connect to a server is grabs this actor and sends the ConnectNow
   * message. It doesn't expect an answer
   */
  val ForceConnectActorName = "StandardActors:ForceConnectActor"

  /**
   * When a client registers itself for a the C2MD service.
   */
  val C2MDRegistration = "StandardActors:C2MDRegistration"


  case object ConnectNow

  case class RegisterC2MD(apiKey: String)

}
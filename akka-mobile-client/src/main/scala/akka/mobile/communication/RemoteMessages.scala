package akka.mobile.communication

/**
 * @author roman.stoffel@gamlor.info
 * @since 24.11.11
 */

/**
 * TODO: Move to Protobuf serialisation. Java serialisation isn't really fun?
 */
object RemoteMessages {

  @SerialVersionUID(1)
  case class RegisterMeForC2MD(id: ClientId, googleClientKey: String)

  @SerialVersionUID(1)
  case class RegisteringDone(id: ClientId, googleClientKey: String)


  @SerialVersionUID(1)
  case class ConnectNow()
}
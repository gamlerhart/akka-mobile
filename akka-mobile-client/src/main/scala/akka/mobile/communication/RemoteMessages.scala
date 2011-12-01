package akka.mobile.communication

/**
 * @author roman.stoffel@gamlor.info
 * @since 24.11.11
 */

/**
 * TODO: Move to Protobuf serialisation. Java serialisation isn't really fun?
 */
object RemoteMessages {

  case class RegisterMeForC2MD(id: ClientId, googleClientKey: String)

  case class RegisteringDone(id: ClientId, googleClientKey: String)

}
package akka.mobile.server

import akka.mobile.communication.ClientId

/**
 * @author roman.stoffel@gamlor.info
 * @since 18.11.11
 */

object ServerEvents {

  case class ClientConnected(clientId: ClientId)

  case class ClientDisconnected(clientId: ClientId)

  case class NewC2MDRegistration(clientId: ClientId, key: String)

}
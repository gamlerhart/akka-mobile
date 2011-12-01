package akka.mobile.server

/**
 * @author roman.stoffel@gamlor.info
 * @since 01.12.11
 */

sealed abstract class ClientConnectionState

sealed abstract class NotConnected extends ClientConnectionState

case object NotConnectedNoC2MD extends ClientConnectionState

case object NotConnectedC2MDAvailable extends ClientConnectionState

case object Connected extends ClientConnectionState
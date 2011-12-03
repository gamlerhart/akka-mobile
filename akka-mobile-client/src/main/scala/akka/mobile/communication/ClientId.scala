package akka.mobile.communication

/**
 * We need to identify clients in order to send messages back.
 * The IP and port are not appropriate, since those can change when crossing
 * network bounderies. For example switching between local Wifi and mobile network.
 * Therefore we identify clients by this abstract id and map this id the the appropriate
 * communication channel.
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

@SerialVersionUID(1)
case class ClientId(clientId: String, applicationId: String) {
  if (null == clientId) throw new IllegalArgumentException("clientId cannot be null")
  if (null == applicationId) throw new IllegalArgumentException("applicationId cannot be null")

}
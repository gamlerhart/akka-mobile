package akka.mobile.remote

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

/**
 * The interface behind which we hide the gritty details of Android =)
 */
trait DeviceOperations {
  def clientId: ClientId
}
package akka.mobile.server

/**
 * @author roman.stoffel@gamlor.info
 * @since 26.11.11
 */

/**
 * Marker trait: When a message is marked with this interface the system will use C2MD to send the message
 * when no other connection is available
 */
trait SentThroughC2MDIfNoConnectionIsAvailable {

}
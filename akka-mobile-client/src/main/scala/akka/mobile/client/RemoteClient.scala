package akka.mobile.client

import akka.actor._
import java.lang.String
import akka.mobile.communication.ClientId

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */


trait RemoteClient {
  def closeConnections()

  /**
   * Usually the client lazy initializes its connection to the server
   * as soon as the first actor needs to talk to the server.
   *
   * However when you want to actively receive 'push' messages from the server
   * you might want to directly connect, even when the client doesn't need to send anything.
   * The calling this method will do that.
   *
   * This method won't throw any exception in case it a cannot connect to the server.
   * It will just send the usual error messages to the error handling actor
   */
  def connectNow(hostname: String = null, port: Int = -1)

  /**
   * Gets a reference to the remote actor for the given service name and server address.
   */
  def actorFor(serviceId: String, hostname: String, port: Int): ActorRef

  /**
   * Gets a reference to the remote actor for the given. It will use the
   *  server specified in the configuration or given when creating the client
   */
  def actorFor(serviceId: String): ActorRef

  /**
   * Register a actor which can receive push messages from the server
   */
  def register(idOfActor: String, actorRef: ActorRef)

  def configuration: MobileConfiguration


  def c2mdRegistrationKey: Option[String]

  def requestC2MDRegistration()

  def clientId: ClientId
}








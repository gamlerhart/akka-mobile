package akka.mobile.server

import akka.mobile.communication.ClientId
import java.util.concurrent.ConcurrentHashMap

/**
 * @author roman.stoffel@gamlor.info
 * @since 26.11.11
 */

trait ClientInfoDatabase {

  def apiKeyFor(clientId: ClientId): Option[String]

  def storeKeyFor(clientId: ClientId, googleKey: String)
}

/**
 * A in memory database doesn't make much sense. However it is useful for testing or
 * to get started with some little experiments
 *
 * The in memory database can be backed up by a traditional database for persisting the information
 * permanently
 */
class InMemoryClientDatabase(fallback: Option[ClientInfoDatabase] = None) extends ClientInfoDatabase {
  private val storage = new ConcurrentHashMap[ClientId, String]()

  def this(fallback: ClientInfoDatabase) = this (Some(fallback))

  def lookInFallback(database: ClientInfoDatabase, clientId: ClientId): Option[String] = {
    database.apiKeyFor(clientId) match {
      case None => None
      case Some(key) => {
        storeKeyFor(clientId, key)
        Some(key)
      }
    }
  }

  def apiKeyFor(clientId: ClientId) = {
    val key = storage.get(clientId)
    if (null == key) {
      fallback match {
        case None => None
        case Some(fb) => lookInFallback(fb, clientId)
      }
    } else {
      Some(key)
    }
  }

  def storeKeyFor(clientId: ClientId, googleKey: String) {
    storage.put(clientId, googleKey)
    fallback.foreach(fb => {
      fb.storeKeyFor(clientId, googleKey)
    })
  }
}
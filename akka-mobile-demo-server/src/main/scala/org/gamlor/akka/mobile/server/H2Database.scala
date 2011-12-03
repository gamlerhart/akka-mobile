package org.gamlor.akka.mobile.server

import akka.mobile.server.ClientInfoDatabase
import akka.mobile.communication.ClientId
import org.scalaquery.ql.extended.ExtendedTable
import org.scalaquery.ql.extended.H2Driver.Implicit._
import org.scalaquery.session.Database.threadLocalSession
import org.scalaquery.session.{Session, Database}
import org.scalaquery.ResultSetInvoker
import org.scalaquery.simple.StaticQuery
import org.scalaquery.ql.Query
import org.h2.jdbc.JdbcSQLException

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.11.11
 */

class H2Database(databaseUrl: String) extends ClientInfoDatabase {

  val ApiKeys = new ExtendedTable[(String, String)]("googleApiKeys") {
    def deviceId = column[String]("deviceId", O.PrimaryKey)

    def apiKey = column[String]("apiKey")

    def * = deviceId ~ apiKey
  }

  private val database: Database = Database.forURL(databaseUrl, driver = "org.h2.Driver")

  init()

  def init() {
    database withSession {
      if (tableDoesNotExist()) {
        ApiKeys.ddl.create
      }
    }
  }

  def apiKeyFor(clientId: ClientId) = {
    database withSession {

      val deviceId = clientId.clientId + "-" + clientId.applicationId
      val query = for {
        a <- ApiKeys if a.deviceId.like(deviceId)
      } yield a.apiKey

      query.firstOption match {
        case None => None
        case Some(key) => Some(key)
      }
    }
  }

  def storeKeyFor(clientId: ClientId, googleKey: String) {

    database withSession {
      val deviceId = clientId.clientId + "-" + clientId.applicationId
      val query = for {
        a <- ApiKeys if a.deviceId.like(deviceId)
      } yield a.apiKey

      query.firstOption match {
        case None => {
          ApiKeys.insert(deviceId, googleKey)
        }
        case Some(_) => {
          query.update(googleKey)
        }
      }
    }

  }

  private def tableDoesNotExist(): Boolean = {
    try {
      Query(ApiKeys).firstOption.getOrElse(("", ""))
      false
    } catch {
      case e: JdbcSQLException => {
        e.printStackTrace()
        true
      }
      case e => throw e
    }
  }
}
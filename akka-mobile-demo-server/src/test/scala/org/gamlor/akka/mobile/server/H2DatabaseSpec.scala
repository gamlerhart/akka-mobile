package org.gamlor.akka.mobile.server

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.mobile.communication.ClientId
import java.util.UUID

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.11.11
 */

class H2DatabaseSpec extends Spec with ShouldMatchers {


  describe("The H2 db") {
    it("should return none if nothing is stored") {
      val theDB = newDB()

      theDB.apiKeyFor(ClientId("device1", "app1")) should be(None)
    }

    it("can store key") {
      val theDB = newDB()

      theDB.storeKeyFor(ClientId("device1", "app1"), "key")
      theDB.apiKeyFor(ClientId("device1", "app1")) should be(Some("key"))
    }

    it("can update key") {
      val theDB = newDB()

      theDB.storeKeyFor(ClientId("device1", "app1"), "old-key")
      theDB.apiKeyFor(ClientId("device1", "app1")) should be(Some("old-key"))
      theDB.storeKeyFor(ClientId("device1", "app1"), "new-key")
      theDB.apiKeyFor(ClientId("device1", "app1")) should be(Some("new-key"))
    }
    it("creates shema once") {
      val db = newDatabaseConnectionString()
      val theDB = new H2Database(db)
      theDB.apiKeyFor(ClientId("device1", "app1")) should be(None)


      val theDB2 = new H2Database(db)
      theDB2.storeKeyFor(ClientId("device1", "app1"), "new-key")
    }
  }

  private def newDatabaseConnectionString(): String = {
    "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"
  }

  private def newDB(): H2Database = {
    new H2Database(newDatabaseConnectionString)
  }

}
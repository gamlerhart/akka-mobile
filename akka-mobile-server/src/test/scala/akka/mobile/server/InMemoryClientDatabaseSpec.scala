package akka.mobile.server

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import akka.mobile.communication.ClientId

/**
 * @author roman.stoffel@gamlor.info
 * @since 27.11.11
 */

class InMemoryClientDatabaseSpec extends Spec with ShouldMatchers {

  describe("The InMemory Database") {
    it("should return none for not stored entries") {
      val database = new InMemoryClientDatabase()
      database.apiKeyFor(ClientId("device1", "app1")) should be(None)
      database.apiKeyFor(ClientId("device1", "app2")) should be(None)
      database.apiKeyFor(ClientId("device2", "app1")) should be(None)
    }
    it("keeps api key") {
      val database = new InMemoryClientDatabase()
      val client1 = ClientId("device1", "app1")
      database.apiKeyFor(client1) should be(None)

      database.storeKeyFor(client1, "key1")
      database.apiKeyFor(client1) should be(Some("key1"))
      database.storeKeyFor(client1, "key2")
      database.apiKeyFor(client1) should be(Some("key2"))
    }
    it("stores also in fallback") {
      val fallBack = new InMemoryClientDatabase()
      val inMemory = new InMemoryClientDatabase(Some(fallBack))

      val client1 = ClientId("device1", "app1")

      inMemory.storeKeyFor(client1, "key1")
      inMemory.apiKeyFor(client1) should be(Some("key1"))
      fallBack.apiKeyFor(client1) should be(Some("key1"))
    }
    it("retrieves from fallback") {
      val fallBack = new InMemoryClientDatabase()

      val client1 = ClientId("device1", "app1")
      fallBack.storeKeyFor(client1, "key1")
      val inMemory = new InMemoryClientDatabase(Some(fallBack))

      inMemory.apiKeyFor(client1) should be(Some("key1"))
    }
  }

}
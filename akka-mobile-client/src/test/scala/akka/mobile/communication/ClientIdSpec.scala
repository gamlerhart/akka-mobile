package akka.mobile.communication

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */

class ClientIdSpec extends Spec with MustMatchers {

  describe("A client id") {
    it("has a client- and application id") {
      val id = ClientId("client-id", "app.id")
      id.clientId must be("client-id")
      id.applicationId must be("app.id")
    }
    it("the client- nor the application id can be null") {
      intercept[IllegalArgumentException] {
        ClientId("client-id", null)
      }
      intercept[IllegalArgumentException] {
        ClientId(null, "app.id")
      }

    }
  }
  describe("Client ids") {
    it("can be compared") {
      val id1 = ClientId("client-id", "app.id")
      val id2 = ClientId("client-id", "app.id")
      val otherApp = ClientId("client-id", "other-app")
      val otherClient = ClientId("other-id", "app.id")



      id1 must be(id2)
      id1.hashCode() must be(id2.hashCode())
      id1 must not be (otherApp)
      id1 must not be (otherClient)
    }
  }

}
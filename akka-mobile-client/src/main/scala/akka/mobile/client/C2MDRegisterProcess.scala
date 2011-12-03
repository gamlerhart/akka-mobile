package akka.mobile.client

import akka.actor.Actor
import java.lang.String
import java.net.InetSocketAddress
import akka.mobile.communication.InternalActors
import akka.mobile.communication.RemoteMessages.{RegisteringDone, RegisterMeForC2MD}
import akka.mobile.client.C2MDRegisterProcess.{IsRegisteredResponse, IsRegisteredRequest, RegisterWith}

/**
 * @author roman.stoffel@gamlor.info
 * @since 25.11.11
 */

class C2MDRegisterProcess(device: DeviceOperations, client: RemoteClient) extends Actor {
  val LastGivenKey = "last-given-google-c2md-key"
  val HostKey = "google-c2md-registration-host"
  val PortKey = "google-c2md-registration-port"
  val RegisteredKey = "registered-google-c2md-key"


  override def preStart() {
    self ! "Check-Resume"
    if (client.configuration.C2MD_REGISTRATION_MODE == "AUTO") {
      self ! "Check-Invite"
    }
  }

  protected def receive = {
    case "Check-Resume" => {
      val lastKey = device.getPreference(LastGivenKey, "no-key");
      val currentKey = device.getPreference(RegisteredKey, "no-key");
      if (lastKey != currentKey) {
        self ! RegisterWith(lastKey)
      }
    }
    case "Check-Invite" => {
      val lastKey = device.getPreference(LastGivenKey, "no-key");
      if ("no-key" == lastKey) {
        client.requestC2MDRegistration()
      }
    }
    case RegisterWith(registrationKey) => {
      client.actorFor(InternalActors.C2MDRegistration) ! RegisterMeForC2MD(device.clientId, registrationKey)

      device.storePreference(wc => {
        wc.put(LastGivenKey, registrationKey)
      })
    }
    case RegisteringDone(id, key) => {
      device.storePreference(wc => {
        wc.put(RegisteredKey, key)
      })
    }
    case IsRegisteredRequest => {
      device.getPreference(RegisteredKey, "") match {
        case "" => self.reply(IsRegisteredResponse(None))
        case x: String => self.reply(IsRegisteredResponse(Some(x)))
      }
    }
  }

}


object C2MDRegisterProcess {

  case class RegisterWith(registrationKey: String)

  case object IsRegisteredRequest

  case class IsRegisteredResponse(apiKey: Option[String])

}

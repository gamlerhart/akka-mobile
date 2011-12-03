package akka.mobile.server

import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol
import akka.mobile.communication.CommunicationMessages.SendMessage
import org.jboss.netty.handler.codec.http._
import java.net.URLEncoder
import akka.mobile.communication.ClientId
import akka.mobile.server.C2MDFailures.{NoC2MDRegistrationForDevice, CouldNotConnectToC2MD}
import java.lang.{Throwable, IllegalStateException}
import com.ning.http.client._
import com.ning.http.client.HttpResponseStatus
import com.ning.http.client.AsyncHandler.STATE
import java.io.IOException

/**
 * @author roman.stoffel@gamlor.info
 * @since 26.11.11
 */

trait FallBackPushMessageSender {
  def canSendC2MDToThisClient(clientId: ClientId): Boolean

  def sendRequest(deviceId: ClientId, message: AkkaMobileProtocol, sender: Option[ActorRef], connectionHandlingActor: ActorRef)
}

object NoC2MDAvailable extends FallBackPushMessageSender {
  def sendRequest(deviceId: ClientId, message: AkkaMobileProtocol, sender: Option[ActorRef], connectionHandlingActor: ActorRef) {
    throw new IllegalStateException("No C2MD service is set up")
  }

  def canSendC2MDToThisClient(clientId: ClientId) = false
}

class C2MDSender(config: ServerConfiguration,
                 database: ClientInfoDatabase,
                 errorHandler: ActorRef) extends FallBackPushMessageSender {


  private val authToken = config.C2MD_APP_KEY.getOrElse(throw new IllegalStateException("Need a c2md app key to use the google service"))
  private val uriString = config.C2MD_URL
  private val client = {
    val builder = new AsyncHttpClientConfig.Builder();
    builder.setMaximumNumberOfRedirects(3)
    new AsyncHttpClient(builder.build());
  }

  def sendRequest(deviceId: ClientId, message: AkkaMobileProtocol,
                  sender: Option[ActorRef], connectionHandlingActor: ActorRef) {
    database.apiKeyFor(deviceId) match {
      case None => {
        errorHandler ! NoC2MDRegistrationForDevice(deviceId,
          SendMessage(message, sender), connectionHandlingActor)

      }
      case Some(googleDeviceKey) => {
        sendRequestToDevice(googleDeviceKey, message, sender, connectionHandlingActor)
      }

    }

  }


  def canSendC2MDToThisClient(clientId: ClientId) = {
    database.apiKeyFor(clientId) match {
      case Some(_) => true
      case None => false
    }
  }

  def close() {
    client.close()
  }

  private def sendRequestToDevice(googleDeviceKey: String, message: AkkaMobileProtocol, sender: Option[ActorRef], connectionHandlingActor: ActorRef) {
    val requestBuilder = client.preparePost(uriString)
      .addHeader(HttpHeaders.Names.CONTENT_TYPE,
      "application/x-www-form-urlencoded;charset=UTF-8")
      .addHeader(HttpHeaders.Names.AUTHORIZATION, "GoogleLogin auth="
      + authToken)

    val messageBase64 = javax.xml.bind.DatatypeConverter.printBase64Binary(message.toByteArray)
    val postDataBuilder = new StringBuilder();
    postDataBuilder.append("registration_id").append("=")
      .append(googleDeviceKey)
    postDataBuilder.append("&collapse_key=0")
    postDataBuilder.append("&data.payload=")
      .append(URLEncoder.encode(messageBase64, "UTF-8"));

    val content = postDataBuilder.toString().getBytes("UTF-8")
    requestBuilder.setBody(content)


    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_LENGTH, Integer.toString(content.length))

    requestBuilder.execute(new AsyncHandler[Unit] {
      val collectErrorInfo = new StringBuilder

      def onCompleted() {
        if (!collectErrorInfo.isEmpty) {
          errorHandler ! CouldNotConnectToC2MD(new C2MDException(collectErrorInfo.toString()),
            SendMessage(message, sender), connectionHandlingActor)

        }
      }


      def onStatusReceived(responseStatus: HttpResponseStatus) = {
        if (responseStatus.getStatusCode == 200) {
          STATE.CONTINUE
        } else {
          collectErrorInfo.append("Unexpected status code. CODE=" + responseStatus.getStatusCode + " " + responseStatus.getStatusText)
          STATE.ABORT
        }
      }

      def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
        val asString = new String(bodyPart.getBodyPartBytes,"UTF8")
        if(asString.contains("Error=")){
          collectErrorInfo.append(asString)
          STATE.ABORT
        } else{
          STATE.CONTINUE
        }
      }

      def onThrowable(t: Throwable) {
        errorHandler ! CouldNotConnectToC2MD(t,
          SendMessage(message, sender), connectionHandlingActor)

      }

      def onHeadersReceived(headers: HttpResponseHeaders) =
        STATE.CONTINUE
    })
  }


}

class C2MDException(msg : String) extends Exception(msg){

}
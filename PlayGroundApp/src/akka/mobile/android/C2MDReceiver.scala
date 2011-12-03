package akka.mobile.android

import android.content.{Intent, Context, BroadcastReceiver}
import akka.mobile.client.{InternalOperations, InternalOperationsProvider, RemoteClient}
import java.net.InetSocketAddress
import java.lang.{String, IllegalStateException}
import android.os.Message
import android.util.{Log, Base64}


/**
 * Receive and dispatches messages from the C2MD to the given actors.
 * This trait should be needs to be inherited for a regular class which then is registered
 * in the Android system to receive the actual events.
 *
 * You need to implement the 'remoteClient' access which should returns your
 * remote client instance which holds the actors.
 *
 * IMPLEMENTATION-Detail: The implementation casts the given remote client to the internal operations
 * interface (InternalOperationsProvider).
 *
 *
 * @author roman.stoffel@gamlor.info
 * @since 24.11.11
 */

trait C2MDReceiver extends BroadcastReceiver {
  val PayLoadKey = "payload"
  val RegistrationKey = "registration_id"

  def onReceive(context: Context, intent: Intent) {
    Log.i("akka-mobile","C2MD-Event handler called with action: "+intent.getAction)
    if (intent.getAction.equals("com.google.android.c2dm.intent.RECEIVE")) {
      dispatchReceive(context, intent)
    } else if (intent.getAction.equals("com.google.android.c2dm.intent.REGISTRATION")) {
      dispatchRegister(context, intent)
    }
  }

  def register(context: Context, registrationKey: String) {
    operationsForClient(context).registerDevice(registrationKey)
  }


  private def dispatchReceive(context: Context, intent: Intent) {
    val base64 = intent.getStringExtra(PayLoadKey)
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    val server = serverAddress(context)
    operationsForClient(context).postMessage(bytes, server.map(s => new InetSocketAddress(s._1, s._2)))
  }

  private def dispatchRegister(context: Context, intent: Intent) {
    val registrationKey = intent.getStringExtra(RegistrationKey)
    if (null != intent.getStringExtra("error")) {
      throw new C2MDException(intent.getStringExtra("error"))
    }
    register(context, registrationKey)
  }

  private def operationsForClient(context: Context) = {
    remoteClient(context).asInstanceOf[InternalOperationsProvider].internalOperationsAccess

  }

  /**
   * The remote client instance which your application is using
   */
  def remoteClient(context: Context): RemoteClient

  /**
   * The hostname and port of the server of the server the client should answer.
   *
   * The default operation will get the host and ip from the configuration of the given remote client.
   * If no host / port are configured there will be no sender in the context.
   * However for registering a C2MD client you need a server.
   * The default implementation will contact the server also for registration. For that case you can overwrite
   * the 'register' method.
   *
   */
  def serverAddress(context: Context): Option[(String, Int)] = {
    val config = remoteClient(context: Context).configuration;
    if (config.HOST.isEmpty || config.PORT.isEmpty) {
      None
    }
    Some(config.HOST.get, config.PORT.get)
  }
}

case class C2MDException(message: String) extends RuntimeException(message)
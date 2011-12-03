package akka.mobile.client

import akka.actor._
import java.net.InetSocketAddress
import java.lang.{IllegalStateException, String}
import akka.mobile.communication.{RemoteMessages, InternalActors}
import akka.mobile.protocol.MobileProtocol.{AkkaMobileProtocol, MobileMessageProtocol}

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */


object MobileRemoteClient {

  /**
   * Creates a new instance.
   */
  def createClient(device: DeviceOperations): RemoteClient
  = new MobileRemoteClient(device)

  /**
   * Creates a new instance.
   */
  def createClient(device: DeviceOperations,
                   hostname: String, port: Int): RemoteClient
  = new MobileRemoteClient(device, Some((hostname, port)))


  def createClient(device: DeviceOperations,
                   socketFactory: InetSocketAddress => SocketRepresentation): RemoteClient
  = new MobileRemoteClient(device, socketFactory = socketFactory)

  def createClient(device: DeviceOperations,
                   hostnameAndPort: Option[(String, Int)] = None,
                   errorHandler: ActorRef = DefaultClientErrorHandler(),
                   configuration: MobileConfiguration = MobileConfiguration.defaultConfig,
                   socketFactory: InetSocketAddress => SocketRepresentation
                   = RemoteMessaging.DEFAULT_TCP_SOCKET_FACTOR): RemoteClient
  = new MobileRemoteClient(device, hostnameAndPort, errorHandler, configuration, socketFactory)
}

class MobileRemoteClient(device: DeviceOperations,
                         hostnameAndPort: Option[(String, Int)] = None,
                         errorHandler: ActorRef = DefaultClientErrorHandler(),
                         val configuration: MobileConfiguration = MobileConfiguration.defaultConfig,
                         socketFactory: InetSocketAddress => SocketRepresentation
                         = RemoteMessaging.DEFAULT_TCP_SOCKET_FACTOR) extends RemoteClient
with InternalOperationsProvider {


  def connectNow(hostname: String = null, port: Int = -1) {
    if (hostname == null || port < 1) {
      actorFor(InternalActors.ForceConnectActorName) ! RemoteMessages.ConnectNow
    } else {
      actorFor(InternalActors.ForceConnectActorName, hostname, port) ! RemoteMessages.ConnectNow
    }
  }

  def closeConnections() {
    remoteMessaging.closeConnections()
  }


  errorHandler.start()
  if (configuration.C2MD_EMAIL.isDefined) {
    InternalOperations.c2mdRegistration.start()

  }
  private val remoteMessaging
  = RemoteMessaging(socketFactory, device.clientId, errorHandler)


  def actorFor(serviceId: String, hostname: String, port: Int) = {
    ClientRemoteActorRef(new InetSocketAddress(hostname, port), Left(serviceId), remoteMessaging.msgSink)
  }

  def actorFor(serviceId: String) = {
    if (hostnameAndPort.isEmpty && (configuration.HOST.isEmpty || configuration.PORT.isEmpty)) {
      throw new IllegalStateException("""You need to specify the hostname and port when creating the remote support in order to use this method.
       You can do this when creating the client instance or in the configuration""")
    }
    val (hostname, ip) = hostnameAndPort.getOrElse((configuration.HOST.get, configuration.PORT.get));
    actorFor(serviceId, hostname, ip)
  }

  def register(idOfActor: String, actorRef: ActorRef) {
    remoteMessaging.registry.registerActor(idOfActor, actorRef)
    actorRef.start()
  }


  def c2mdRegistrationKey = {
    val result = (InternalOperations.c2mdRegistration ? C2MDRegisterProcess.IsRegisteredRequest).get
    result.asInstanceOf[C2MDRegisterProcess.IsRegisteredResponse].apiKey
  }


  def requestC2MDRegistration() = {
    val email = configuration.C2MD_EMAIL
      .getOrElse(throw new IllegalStateException("Need a email in the configuration for C2MD"))
    device.registerForC2MD(email)
  }


  def clientId = device.clientId

  def internalOperationsAccess: InternalOperations = InternalOperations

  object InternalOperations extends InternalOperations {

    val c2mdRegistration = Actor.actorOf(new C2MDRegisterProcess(device, MobileRemoteClient.this)).start()

    def postMessage(messageBytes: Array[Byte], server: Option[InetSocketAddress]) {
      remoteMessaging.wireMsgDispatcher.dispatchMessage(
        AkkaMobileProtocol.parseFrom(messageBytes).getMessage, server.map(s => Right(s)));
    }

    def registerDevice(registrationKey: String) {
      c2mdRegistration ! C2MDRegisterProcess.RegisterWith(registrationKey)
    }
  }

}

/**
 * Entry point for internal operations. Like bridge-operations between Android and our internal implementations
 */
trait InternalOperationsProvider {

  def internalOperationsAccess: InternalOperations
}

trait InternalOperations {

  def postMessage(messageBytes: Array[Byte], server: Option[InetSocketAddress]): Unit

  def registerDevice(registrationKey: String): Unit
}





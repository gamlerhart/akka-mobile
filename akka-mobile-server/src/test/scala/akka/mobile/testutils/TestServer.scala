package akka.mobile.testutils

import akka.actor.{Actor, ActorRef}
import java.net.InetSocketAddress
import akka.mobile.server._
import akka.mobile.client.{MobileConfiguration, MobileRemoteClient}

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

object TestServer {

  def withRunningServer(toRun: TestServerContext => Unit) {
    withRunningServer(BlackholeActor(), toRun)
  }

  def withRunningServer(errorHandler: ActorRef, toRun: TestServerContext => Unit) {
    val port = NetworkUtils.findFreePort()
    val database = new InMemoryClientDatabase()
    val server = NettyRemoteServer.start("localhost", port, errorHandler, Some(database), TestConfigs.defaultServer);
    toRun(TestServerContext(server, database, port))
    server.shutdownServerModule();
  }

}

case class TestServerContext(server: RemoteServer, database: ClientInfoDatabase, port: Int) {
  val address: InetSocketAddress = new InetSocketAddress("localhost", port)

  def registerStandardEchoActor(): String = {
    val name = "echo"
    server.register(name, Actor.actorOf(new EchoActor()))
    name
  }


  def register(id: String, actor: ActorRef) {
    server.register(id, actor)
  }

  def connectAClient(config: MobileConfiguration = MobileConfiguration.defaultConfig) = {
    MobileRemoteClient.createClient(TestDevice.uniqueTestDevice(), Some("localhost", port), configuration = config)
  }


}
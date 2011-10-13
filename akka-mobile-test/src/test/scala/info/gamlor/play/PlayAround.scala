package info.gamlor.play

import akka.actor.Actor
import akka.remote.netty.NettyRemoteSupport

/**
 * @author roman.stoffel@gamlor.info
 * @since 13.10.11
 */

object PlayAround extends App {
  val r1 = new NettyRemoteSupport()
  r1.start()
  r1.registerPerSession("hello-service",Actor.actorOf[SimulationActor])

  try{
  Actor.remote.start();

  }catch {
    case e:Exception => e.printStackTrace()
  }


  //r1.start("localhost",2552);

}

class SimulationActor extends  Actor{
  protected def receive = {
    case "Hello" =>{
      println("Hell Yeah")
    }
  }
}
package info.gamlor.remoting


/**
 * @author roman.stoffel@gamlor.info
 * @since 21.10.11
 */
//
//class RunServer extends Spec {
//
//  describe("Test Server") {
//    it("is running") {
//      val server = NettyRemoteServer.start("localhost", 2552);
//
//
//      val local = Actor.actorOf(new ReceiveCheckActor(None)).start();
//      server.register("echo", local);
//
//    }
//  }
//}
//
//class PlayAround extends Spec{
//
//
//  describe("Test Server") {
//    it("dum di dum"){
//      val server = new ServerSocket(8989);
//      val t = new Thread(){
//        override def run() {
//          val s: Socket = server.accept()
//          s.getInputStream;
//          while(true){
//            println("Av"+s.getInputStream.available())
//            Thread.`yield`()
//          }
//        }
//      }
//      t.start()
//      val s = new Socket();
//      s.connect(new InetSocketAddress("localhost",8989));
//
//      try{
//      while(true){
//      s.getOutputStream.write(2)
//        s.getOutputStream.flush()
//
//      }}catch{
//        case e:Exception=>{
//          e.printStackTrace()
//        }
//      }
//    }}

//}



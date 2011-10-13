package info.gamlor.akkamobile

import android.app.Activity
import akka.actor.Actor
import com.eaio.uuid.UUID
import android.util.Log
import android.os.{Handler, Bundle}
import akka.event.EventHandler
import java.net.{Socket, ServerSocket, InetSocketAddress}
import java.io.PrintWriter

class AkkaDroidApp extends Activity {

  /**Called when the activity is first created. */
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    val socket = new Socket("www.gamlor.info",80)
    val out = new PrintWriter(socket.getOutputStream, true);
    out.write("Hi")
    out.flush();

//    val h = new Handler()
//    val errorHandlerEventListener = Actor.actorOf(new Actor {
//      self.dispatcher = EventHandler.EventHandlerDispatcher
//
//      def receive = {
//        case EventHandler.Debug(instance, message) => {
//          Log.e("Debug", message.toString)
//        }
//        case _ => {}
//      }
//    })
//    EventHandler.addListener(errorHandlerEventListener);
//    //val id = new UUID()
//    //Log.i("debug",id.toString)
//
//    Actor.remote.start("0.0.0.0", freePort());
//    val actor = Actor.actorOf(new MyActor(h, this))
//    actor.start()
//    actor ! "Start"
  }

}


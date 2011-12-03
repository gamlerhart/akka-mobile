package info.gamlor.akkamobile

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.{TextView, EditText, Button}
import android.text.method.ScrollingMovementMethod
import akka.mobile.android.ActivityActor
import info.gamlor.playaround.{AddMessageToChat, SendMessageToChat}
import info.gamlor.akkamobile.MyApplication._
import android.app.Activity
import akka.actor.{Actor, ActorRef}
import android.util.Log

class AkkaDroidApp extends Activity with ActivityActor {

  private var chatServiceActor: ActorRef = null;


  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    chatServiceActor = getApplication.remote.actorFor("chat-service")
    //getApplication.remote.requestC2MDRegistration()

    //getApplication.remote.connectNow()
    getApplication.remote.register("notifications", Actor.actorOf(new Actor() {
      protected def receive = {
        case s: String => {
          Log.i("akka-mobile",">>>>>"+s)
        }
      }
    }).start())

    findViewById(R.id.chatMsgs).asInstanceOf[TextView].setMovementMethod(new ScrollingMovementMethod())


    val sendButton = findViewById(R.id.sendButton).asInstanceOf[Button]
    sendButton.setOnClickListener(new OnClickListener {
      def onClick(p1: View) {
        val inputTextBox: EditText = findViewById(R.id.enterMsgBox).asInstanceOf[EditText]
        val message = inputTextBox.getText
        self ! message.toString
        inputTextBox.setText("")
      }
    })
  }

  protected def receive = {
    case message: String => {
      chatServiceActor ! SendMessageToChat(message)
    }
    case AddMessageToChat(message) => {
      val inputTextBox: EditText = findViewById(R.id.enterMsgBox).asInstanceOf[EditText]
      val chatList = findViewById(R.id.chatMsgs).asInstanceOf[TextView]
      chatList.append("\n" + message)
      inputTextBox.setText("")
    }
  }
}



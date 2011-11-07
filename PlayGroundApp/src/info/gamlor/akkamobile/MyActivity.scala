package info.gamlor.akkamobile

import android.app.Activity
import android.os.Bundle
import android.util.Log

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

class MyActivity extends Activity {

  override def onCreate(savedInstanceState: Bundle) {
    Log.i("hi", "info")
  }

}
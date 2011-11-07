package akka.mobile.testsuite

import android.test.ActivityInstrumentationTestCase2
import info.gamlor.akkamobile.MyActivity
import junit.framework.Assert

/**
 * @author roman.stoffel@gamlor.info
 * @since 06.11.11
 */

class MyActivityTest
  extends ActivityInstrumentationTestCase2[MyActivity]("info.gamlor.akkamobile", classOf[MyActivity]) {


  def testPreconditions() {

  }

  def testPreconditions2() {
    Assert.assertTrue(true)
  }
}
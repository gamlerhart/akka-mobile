package info.gamlor.playaround

import java.io.{InputStreamReader, BufferedReader}
import java.net.{URLEncoder, HttpURLConnection, URL}
import javax.net.ssl.{SSLSession, HostnameVerifier, HttpsURLConnection}


/**
 * @author roman.stoffel@gamlor.info
 * @since 18.11.11
 */

object C2MDExperiments extends App {

  val PARAM_REGISTRATION_ID = "registration_id"
  val PARAM_COLLAPSE_KEY = "collapse_key"
  val UTF8 = "UTF-8"

  main()

  def main() :Int={
    val postDataBuilder = new StringBuilder();
    postDataBuilder.append(PARAM_REGISTRATION_ID).append("=")
      .append("APA91bHdAm456CVzhKyZH-DDaq6zMdr7R5hzuBdvRxJoi7w1L2zNAKMz33mB2PGU9eaUulblFB0Tb1mP2iiS0b4d8gUyvWpWARh_C4HHt0L83v0u7qr-Ni2q8UkIRnxbDTDziFcJf_fLU1Bu8Ms25Cd9V0hj3N8u5A");
    postDataBuilder.append("&").append(PARAM_COLLAPSE_KEY).append("=")
      .append("0");
    postDataBuilder.append("&").append("data.payload").append("=")
      .append(URLEncoder.encode("DURING REBOOT", UTF8));

    val postData = postDataBuilder.toString().getBytes(UTF8);

    // Hit the dm URL.

    val url = new URL("https://android.apis.google.com/c2dm/send");
    HttpsURLConnection
      .setDefaultHostnameVerifier(new CustomizedHostnameVerifier());
    val conn = url.openConnection().asInstanceOf[HttpsURLConnection];
    conn.setDoOutput(true);
    conn.setUseCaches(false);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type",
      "application/x-www-form-urlencoded;charset=UTF-8");
    conn.setRequestProperty("Content-Length",
      Integer.toString(postData.length));
//    conn.setRequestProperty("Authorization", "GoogleLogin auth="
//      + "DQAAAN0AAADLAKYLxj0iftwzgCcFjyY2bn89Va1003Y25KHz0RwJJo1-sLP21UthCSVNPRtvuPf");

    val out = conn.getOutputStream();
    out.write(postData);
    out.close();

    val responseCode = conn.getResponseCode();
    println(responseCode)
    return responseCode;
  }

  class CustomizedHostnameVerifier extends HostnameVerifier {
    def verify(hostname: String, session: SSLSession) = {
      true;
    }
  }

}

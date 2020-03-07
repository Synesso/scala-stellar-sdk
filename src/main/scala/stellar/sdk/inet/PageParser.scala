package stellar.sdk.inet

import java.net.HttpURLConnection.{HTTP_BAD_REQUEST, HTTP_NOT_FOUND}

import okhttp3.HttpUrl
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.JsonMethods

import scala.reflect.ClassTag

object PageParser {

  def parse[T: ClassTag](url: HttpUrl, responseCode: Int, body: => String)
                        (implicit m: Manifest[T], customFormats: Formats): Page[T] = {

    responseCode match {
      case HTTP_NOT_FOUND => Page(List.empty[T], None)
      case HTTP_BAD_REQUEST => throw HorizonBadRequest(url, body)
      case _ =>
        JsonMethods.parse(body)
          .extract[RawPage]
          .parse[T](url)
    }
  }
}

package stellar.sdk.inet

import okhttp3.HttpUrl
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats, JObject, JValue}

import scala.concurrent.duration.Duration
import scala.util.Try

case class HorizonServerError(uri: HttpUrl, body: JObject)(implicit val formats: Formats) extends Exception(
  s"Server error when communicating with Horizon. $uri -> ${(body \ "detail").extract[String]}"
)

case class HorizonEntityNotFound(uri: HttpUrl, body: JValue)(implicit val formats: Formats) extends Exception(
  s"Requested entity was not found in Horizon. $uri -> ${(body \ "detail").extract[String]}"
)

case class HorizonRateLimitExceeded(uri: HttpUrl, retryAfter: Duration)(implicit val formats: Formats) extends Exception(
  s"Horizon request rate limit was exceeded. Try again in $retryAfter"
)

case class HorizonBadRequest(uri: HttpUrl, body: String) extends Exception(
  s"Bad request. $uri -> ${
    implicit val formats: Formats = DefaultFormats
    Try(
      (JsonMethods.parse(body) \ "extras" \ "reason").extract[String]
    ).getOrElse(body)
  }")

case class FailedResponse(cause: String) extends Exception(cause)

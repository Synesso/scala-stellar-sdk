package stellar.sdk.inet

import okhttp3.HttpUrl
import org.json4s.{Formats, JObject}

import scala.concurrent.duration.Duration

case class HorizonServerError(uri: HttpUrl, body: JObject)(implicit val formats: Formats) extends Exception(
  s"Server error when communicating with Horizon. $uri -> ${(body \ "detail").extract[String]}"
)

case class HorizonEntityNotFound(uri: HttpUrl, body: JObject)(implicit val formats: Formats) extends Exception(
  s"Requested entity was not found in Horizon. $uri -> ${(body \ "detail").extract[String]}"
)

case class HorizonRateLimitExceeded(uri: HttpUrl, retryAfter: Duration)(implicit val formats: Formats) extends Exception(
  s"Horizon request rate limit was exceeded. Try again in $retryAfter"
)

case class FailedResponse(cause: String) extends Exception(cause)

package stellar.sdk.inet

import akka.http.scaladsl.model.Uri
import org.json4s.{Formats, JObject}

import scala.concurrent.duration.Duration

case class HorizonServerError(uri: Uri, body: JObject)(implicit val formats: Formats) extends Exception(
  s"Server error when communicating with Horizon. $uri -> ${(body \ "detail").extract[String]}"
)

case class HorizonEntityNotFound(uri: Uri, body: JObject)(implicit val formats: Formats) extends Exception(
  s"Requested entity was not found in Horizon. $uri -> ${(body \ "detail").extract[String]}"
)

case class HorizonRateLimitExceeded(uri: Uri, retryAfter: Duration)(implicit val formats: Formats) extends Exception(
  s"Horizon request rate limit was exceeded. Try again in $retryAfter"
)

case class FailedResponse(cause: String) extends Exception(cause)

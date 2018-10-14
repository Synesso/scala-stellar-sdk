package stellar.sdk.inet

import akka.http.scaladsl.model.Uri
import org.json4s.{Formats, JObject}

case class HorizonServerError(uri: Uri, body: JObject)(implicit val formats: Formats) extends Exception(
  s"Server error when communicating with Horizon. $uri -> ${(body \ "detail").extract[String]}"
)

case class HorizonEntityNotFound(uri: Uri, body: JObject)(implicit val formats: Formats) extends Exception(
  s"Requested entity was not found in Horizon. $uri -> ${(body \ "detail").extract[String]}"
)
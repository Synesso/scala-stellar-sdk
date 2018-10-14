package stellar.sdk.inet

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import org.json4s.{Formats, JObject}

case class HorizonServerError(uri: Uri, body: String) extends Exception(
  s"Server error when communicating with Horizon. $uri -> $body"
)

case class HorizonEntityNotFound(uri: Uri, body: JObject)(implicit val formats: Formats) extends Exception(
  s"Requested entity was not found in Horizon. $uri -> ${(body \ "detail").extract[String]}"
)
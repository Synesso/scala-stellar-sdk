package stellar.sdk

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import stellar.sdk.inet.WebClient
import stellar.sdk.model.response.{FederationResponse, FederationResponseDeserialiser}

import scala.concurrent.{ExecutionContext, Future}

class FederationServer(val base: Uri, path: Path)
                      (implicit val system: ActorSystem = ActorSystem("stellar-sdk", ConfigFactory.load().getConfig("scala-stellar-sdk")))
  extends WebClient with LazyLogging {

  implicit val formats = Serialization.formats(NoTypeHints) + FederationResponseDeserialiser

  def byName(name: String)(implicit ec: ExecutionContext): Future[Option[FederationResponse]] = {
    get[Option[FederationResponse]](path, Map("q" -> name, "type" -> "name"))
  }
}

object FederationServer {
  def apply(uriString: String): FederationServer = {
    val uri = Uri(uriString)
    new FederationServer(uri.withPath(Path.Empty), uri.path)
  }
}
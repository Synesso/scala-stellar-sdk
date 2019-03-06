package stellar.sdk

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import stellar.sdk.inet.{RestException, WebClient}
import stellar.sdk.model.response.{FederationResponse, FederationResponseDeserialiser}

import scala.concurrent.{ExecutionContext, Future}

case class FederationServer(base: Uri, path: Path)
                      (implicit val system: ActorSystem = ActorSystem("stellar-sdk", ConfigFactory.load().getConfig("scala-stellar-sdk")))
  extends WebClient(base) with LazyLogging {

  implicit val formats = Serialization.formats(NoTypeHints) + FederationResponseDeserialiser
  import HalJsonSupport._

  def byName(name: String)(implicit ec: ExecutionContext): Future[Option[FederationResponse]] = {
    get[FederationResponse](path, Map("q" -> name, "type" -> "name"))
      .map(_.map(_.copy(address = name)).map(validate))
  }

  def byAccount(account: PublicKey)(implicit ec: ExecutionContext): Future[Option[FederationResponse]] = {
    get[FederationResponse](path, Map("q" -> account.accountId, "type" -> "id"))
      .map(_.map(_.copy(account = account)).map(validate))
  }

  private def validate(fr: FederationResponse): FederationResponse = {
    if (fr.account == null) throw RestException(s"Document did not contain account_id")
    if (fr.address == null) throw RestException(s"Document did not contain stellar_address")
    fr
  }
}

object FederationServer {
  def apply(uriString: String): FederationServer = {
    val uri = Uri(uriString)
    new FederationServer(uri.withPath(Path.Empty), uri.path)
  }
}
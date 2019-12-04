package stellar.sdk

import java.net.HttpURLConnection.HTTP_NOT_FOUND

import com.typesafe.scalalogging.LazyLogging
import okhttp3.{Headers, HttpUrl, OkHttpClient, Request}
import org.json4s.native.{JsonMethods, Serialization}
import org.json4s.{Formats, NoTypeHints}
import stellar.sdk.inet.RestException
import stellar.sdk.model.response.{FederationResponse, FederationResponseDeserialiser}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class FederationServer(base: HttpUrl) extends LazyLogging {

  implicit val formats: Formats = Serialization.formats(NoTypeHints) + FederationResponseDeserialiser
  private val client = new OkHttpClient()
  private val headers = Headers.of(
    "X-Client-Name", BuildInfo.name,
    "X-Client-Version", BuildInfo.version)

  def byName(name: String)(implicit ec: ExecutionContext): Future[Option[FederationResponse]] =
    fetchFederationResponse(base.newBuilder()
      .addQueryParameter("q", name)
      .addQueryParameter("type", "name")
      .build(),  _.copy(address = name))

  def byAccount(account: PublicKey)(implicit ec: ExecutionContext): Future[Option[FederationResponse]] =
    fetchFederationResponse(base.newBuilder()
      .addQueryParameter("q", account.accountId)
      .addQueryParameter("type", "id")
      .build(), _.copy(account = account))


  private def fetchFederationResponse(url: HttpUrl, fillIn: FederationResponse => FederationResponse)
                                     (implicit ec: ExecutionContext): Future[Option[FederationResponse]] =
    Future(client.newCall(new Request.Builder().url(url).headers(headers).build()).execute())
      .map { response =>
        response.code() match {
          case HTTP_NOT_FOUND => None
          case e if e >= 500 => throw RestException(response.body().string())
          case _ =>
            Try(response.body().string())
              .map(JsonMethods.parse(_))
              .map(_.extract[FederationResponse])
              .map(fillIn)
              .map(validate) match {
              case Success(fr) => Some(fr)
              case Failure(t) => throw RestException("Could not parse document as FederationResponse.", t)
            }
        }
      }


  private def validate(fr: FederationResponse): FederationResponse = {
    if (fr.account == null) throw RestException(s"Document did not contain account_id")
    if (fr.address == null) throw RestException(s"Document did not contain stellar_address")
    fr
  }
}

object FederationServer {
  def apply(uriString: String): FederationServer = new FederationServer(HttpUrl.parse(uriString))
}
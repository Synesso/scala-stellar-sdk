package stellar.sdk.model.domain

import java.net.HttpURLConnection.HTTP_NOT_FOUND

import okhttp3.{Headers, HttpUrl, OkHttpClient, Request}
import stellar.sdk.inet.RestException
import stellar.sdk.{BuildInfo, FederationServer, PublicKey}
import toml.Value
import toml.Value.{Arr, Str, Tbl}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

/**
  * Data provided by a domain's `stellar.toml`.
  *
  * @see https://www.stellar.org/developers/guides/concepts/stellar-toml.html
  */
case class DomainInfo(
  federationServer: Option[FederationServer] = None,
  authServer: Option[HttpUrl] = None,
  transferServer: Option[HttpUrl] = None,
  kycServer: Option[HttpUrl] = None,
  webAuthEndpoint: Option[HttpUrl] = None,
  signerKey: Option[PublicKey] = None,
  horizonEndpoint: Option[HttpUrl] = None,
  uriRequestSigningKey: Option[PublicKey] = None,
  version: Option[String] = None,
  accounts: List[PublicKey] = List.empty[PublicKey],
  issuerDocumentation: Option[IssuerDocumentation] = None,
  pointsOfContact: List[PointOfContact] = Nil,
  currencies: List[Currency] = Nil,
  validators: List[Validator] = Nil,
)

object DomainInfo extends TomlParsers {

  private val client = new OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .build()
  private val headers = Headers.of(
    "X-Client-Name", BuildInfo.name,
    "X-Client-Version", BuildInfo.version)

  def from(doc: String): DomainInfo = {
    toml.Toml.parse(doc) match {
      case Left((address, msg)) => throw DomainInfoParseException(msg, address)
      case Right(tbl) =>

        def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
          super.parseTomlValue(tbl, key, parser)

        DomainInfo(
          federationServer = parseTomlValue("FEDERATION_SERVER", { case Str(s) => FederationServer(s) }),
          authServer = parseTomlValue("AUTH_SERVER", url),
          transferServer = parseTomlValue("TRANSFER_SERVER", url),
          kycServer = parseTomlValue("KYC_SERVER", url),
          webAuthEndpoint = parseTomlValue("WEB_AUTH_ENDPOINT", url),
          signerKey = parseTomlValue("SIGNER_KEY", publicKey),
          horizonEndpoint = parseTomlValue("HORIZON_URL", url),
          uriRequestSigningKey = parseTomlValue("URI_REQUEST_SIGNING_KEY", publicKey),
          version = parseTomlValue("VERSION", string),
          accounts = parseTomlValue("ACCOUNTS", { case Arr(xs) => xs.map(publicKey) })
              .getOrElse(Nil),
          issuerDocumentation = parseTomlValue("DOCUMENTATION", { case tbl: Tbl => IssuerDocumentation.parse(tbl) }),
          pointsOfContact = parseTomlValue("PRINCIPALS", { case Arr(values) =>
            values.map{ case tbl: Tbl  => PointOfContact.parse(tbl) }}
          ).getOrElse(List.empty),
          currencies = parseTomlValue("CURRENCIES", { case Arr(values) =>
              values.map { case tbl: Tbl => Currency.parse(tbl) }
          }).getOrElse(List.empty),
          validators = parseTomlValue("VALIDATORS", { case Arr(values) =>
              values.map { case tbl: Tbl => Validator.parse(tbl) }
          }).getOrElse(List.empty)
        )
    }
  }



  /**
    * Returns domain info for the given domain's base URI string.
    */
  def forDomain(
    uri: String,
    useHttps: Boolean = true,
    port: Int = 443
  )(implicit ec: ExecutionContext): Future[Option[DomainInfo]] = {
    val url = HttpUrl.parse(if (uri.startsWith("http")) uri else s"http${if (useHttps) "s" else ""}://$uri:$port")
        .newBuilder()
        .addPathSegment(".well-known")
        .addPathSegment("stellar.toml")
        .build()

    Future(client.newCall(new Request.Builder().url(url).headers(headers).build()).execute())
      .map { response =>
        response.code() match {
          case HTTP_NOT_FOUND => None
          case _ =>
            val body = response.body().string()
            Try(DomainInfo.from(body)) match {
              case Failure(t) => throw RestException(s"Error parsing domain info: $body", t)
              case s => s.toOption
            }
        }
      }
  }
}

/**
  * The document could not be parsed into a DomainInfo instance.
  *
  * @param msg the reason the parsing failed.
  */
case class DomainInfoParseException(msg: String, address: List[String] = Nil) extends Exception(
  s"$msg${
    address match {
      case Nil => ""
      case _ => address.mkString(" at ", "/", ".")
    }
  }"
)
package stellar.sdk.model.domain

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import stellar.sdk.inet.WebClient
import stellar.sdk.{DefaultActorSystem, FederationServer, KeyPair, PublicKey}
import toml.Value
import toml.Value.{Arr, Str, Tbl}

import scala.concurrent.Future

/**
  * Data provided by a domain's `stellar.toml`.
  *
  * @see https://www.stellar.org/developers/guides/concepts/stellar-toml.html
  */
case class DomainInfo(federationServer: Option[FederationServer] = None,
                      authServer: Option[Uri] = None,
                      transferServer: Option[Uri] = None,
                      kycServer: Option[Uri] = None,
                      webAuthEndpoint: Option[Uri] = None,
                      signerKey: Option[PublicKey] = None,
                      horizonEndpoint: Option[Uri] = None,
                      uriRequestSigningKey: Option[PublicKey] = None,
                      version: Option[String] = None,
                      accounts: List[PublicKey] = List.empty[PublicKey],
                      issuerDocumentation: Option[IssuerDocumentation] = None,
                      pointsOfContact: List[PointOfContact] = Nil,
                      currencies: List[Currency] = Nil,
                      validators: List[Validator] = Nil,
                     )

object DomainInfo extends TomlParsers {

  implicit private val unmarshaller: FromEntityUnmarshaller[DomainInfo] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`text/plain(UTF-8)`, `application/octet-stream`)
      .mapWithCharset {
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }.map(from)

  def from(doc: String): DomainInfo = {
    toml.Toml.parse(doc) match {
      case Left(msg) => throw DomainInfoParseException(msg)
      case Right(tbl) =>

        def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
          super.parseTomlValue(tbl, key, parser)

        DomainInfo(
          federationServer = parseTomlValue("FEDERATION_SERVER", { case Str(s) => FederationServer(s) }),
          authServer = parseTomlValue("AUTH_SERVER", uri),
          transferServer = parseTomlValue("TRANSFER_SERVER", uri),
          kycServer = parseTomlValue("KYC_SERVER", uri),
          webAuthEndpoint = parseTomlValue("WEB_AUTH_ENDPOINT", uri),
          signerKey = parseTomlValue("SIGNER_KEY", publicKey),
          horizonEndpoint = parseTomlValue("HORIZON_URL", uri),
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
  def forDomain(uri: String)
               (implicit sys: ActorSystem = DefaultActorSystem.system): Future[Option[DomainInfo]] = {

    implicit val ec = sys.dispatcher

    lazy val client = new WebClient(Uri(uri))

    client.get[DomainInfo](Uri.Path("/.well-known/stellar.toml"))
  }
}

/**
  * The document could not be parsed into a DomainInfo instance.
  * @param msg the reason the parsing failed.
  */
case class DomainInfoParseException(msg: String) extends Exception(msg)
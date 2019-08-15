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
                     )

object DomainInfo {

  implicit private val unmarshaller: FromEntityUnmarshaller[DomainInfo] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`text/plain(UTF-8)`, `application/octet-stream`)
      .mapWithCharset {
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }.map(from)

  private[sdk] def from(doc: String): DomainInfo = {
    toml.Toml.parse(doc) match {
      case Left(msg) => throw DomainInfoParseException(msg)
      case Right(tbl) =>

        def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
          tbl.values.get(key).map(parser.applyOrElse(_, {
            v: Value => throw DomainInfoParseException(s"value for $key was not of the expected type. [value=$v]")
          }))

        DomainInfo(
          federationServer = parseTomlValue("FEDERATION_SERVER", { case Str(s) => FederationServer(s) }),
          authServer = parseTomlValue("AUTH_SERVER", { case Str(s) => Uri(s) }),
          transferServer = parseTomlValue("TRANSFER_SERVER", { case Str(s) => Uri(s) }),
          kycServer = parseTomlValue("KYC_SERVER", { case Str(s) => Uri(s) }),
          webAuthEndpoint = parseTomlValue("WEB_AUTH_ENDPOINT", { case Str(s) => Uri(s) }),
          signerKey = parseTomlValue("SIGNER_KEY", { case Str(s) => KeyPair.fromAccountId(s) }),
          horizonEndpoint = parseTomlValue("HORIZON_URL", { case Str(s) => Uri(s) }),
          uriRequestSigningKey = parseTomlValue("URI_REQUEST_SIGNING_KEY", { case Str(s) => KeyPair.fromAccountId(s) }),
          version = parseTomlValue("VERSION", { case Str(s) => s }),
          accounts = parseTomlValue("ACCOUNTS", { case Arr(xs) => xs.map { case Str(s) => KeyPair.fromAccountId(s) }})
              .getOrElse(Nil),
          issuerDocumentation = parseTomlValue("DOCUMENTATION", { case tbl: Tbl => IssuerDocumentation.parse(tbl) })
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
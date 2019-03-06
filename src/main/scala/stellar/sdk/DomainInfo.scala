package stellar.sdk

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import stellar.sdk.inet.WebClient
import toml.Value.Str

import scala.concurrent.Future

/**
  * Data provided by a domain's `stellar.toml`.
  *
  * @see https://www.stellar.org/developers/guides/concepts/stellar-toml.html
  */
case class DomainInfo(federationServer: FederationServer) {

}

object DomainInfo {

  implicit private val unmarshaller: FromEntityUnmarshaller[DomainInfo] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`text/plain(UTF-8)`, `application/octet-stream`)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }.map(from)

  private[sdk] def from(doc: String): DomainInfo = {
    toml.Toml.parse(doc) match {
      case Left(msg) => throw DomainInfoParseException(msg, doc)
      case Right(tbl) =>
        tbl.values.get("FEDERATION_SERVER") match {
          case Some(Str(fs)) => DomainInfo(FederationServer(fs))
          case Some(_) => throw DomainInfoParseException("value for FEDERATION_SERVER was not a String", doc)
          case None => throw DomainInfoParseException("No entry for FEDERATION_SERVER", doc)
        }
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
  * @param doc the document that could not be parsed.
  */
case class DomainInfoParseException(msg: String, doc: String) extends Exception(msg)
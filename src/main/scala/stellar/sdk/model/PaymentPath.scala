package stellar.sdk.model

import org.json4s.JsonAST.JObject
import org.json4s.{DefaultFormats, Formats}
import stellar.sdk.KeyPair
import stellar.sdk.model.response.ResponseParser

case class PaymentPath(source: Amount, destination: Amount, path: Seq[Asset])

object PaymentPathDeserializer extends ResponseParser[PaymentPath]({
  o: JObject =>
    implicit val formats = DefaultFormats
    implicit val assetDeserializer = AmountParser.assetDeserializer
    
    PaymentPath(
      source = AmountParser.amount("source_", o),
      destination = AmountParser.amount("destination_", o),
      path = (o \ "path").extract[Seq[Asset]]
    )
})

object AmountParser {

  implicit val formats = DefaultFormats

  def parseAsset(prefix: String, o: JObject)(implicit formats: Formats): Asset = {
    val assetType = (o \ s"${prefix}asset_type").extract[String]
    def code = (o \ s"${prefix}asset_code").extract[String]
    def issuer = KeyPair.fromAccountId((o \ s"${prefix}asset_issuer").extract[String])
    assetType match {
      case "native" => NativeAsset
      case "credit_alphanum4" => IssuedAsset4(code, issuer)
      case "credit_alphanum12" => IssuedAsset12(code, issuer)
      case t => throw new RuntimeException(s"Unrecognised ${prefix}asset type: $t")
    }
  }

  def amount(prefix: String, o: JObject)(implicit formats: Formats): Amount = {
    val asset = parseAsset(prefix, o)
    val units = Amount.toBaseUnits((o \ s"${prefix}amount").extract[String]).get
    Amount(units, asset)
  }

  val assetDeserializer = new ResponseParser[Asset](parseAsset("", _))
}
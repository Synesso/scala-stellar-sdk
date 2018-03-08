package stellar.sdk.resp

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk._

case class OfferResp(id: Long, seller: PublicKeyOps, selling: Amount, buying: Asset, price: Price)

object OfferRespDeserializer extends CustomSerializer[OfferResp](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats
    val id = (o \ "id").extract[Long]

    def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

    def asset(prefix: String = "", issuerKey: String = "asset_issuer") = {
      def assetCode = (o \ prefix \ "asset_code").extract[String]

      def assetIssuer = KeyPair.fromAccountId((o \ prefix \ issuerKey).extract[String])

      (o \ prefix \ "asset_type").extract[String] match {
        case "native" => NativeAsset
        case "credit_alphanum4" => IssuedAsset4(assetCode, assetIssuer)
        case "credit_alphanum12" => IssuedAsset12(assetCode, assetIssuer)
        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
      }
    }

    def doubleFromString(key: String) = (o \ key).extract[String].toDouble

    def amount(prefix: String = "") = {
      val units = Amount.toBaseUnits(doubleFromString("amount")).get
      asset(prefix) match {
        case nna: NonNativeAsset => IssuedAmount(units, nna)
        case NativeAsset => NativeAmount(units)
      }
    }

    def price = {
      val priceObj = o \ "price_r"
      Price(
        (priceObj \ "n").extract[Int],
        (priceObj \ "d").extract[Int]
      )
    }

    OfferResp(id, account("seller"), amount("selling"), asset("buying"), price)
}, PartialFunction.empty)
)


package stellar.sdk

import java.time.ZonedDateTime

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}

case class Trade(id: String, ledgerCloseTime: ZonedDateTime, offerId: Long,
                 baseAccount: PublicKeyOps, baseAmount: Amount,
                 counterAccount: PublicKeyOps, counterAmount: Amount,
                 baseIsSeller: Boolean)


object TradeDeserializer extends CustomSerializer[Trade](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats

    def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

    def date(key: String) = ZonedDateTime.parse((o \ key).extract[String])

    def doubleFromString(key: String) = (o \ key).extract[String].toDouble

    def asset(prefix: String = "", issuerKey: String = "asset_issuer") = {
      def assetCode = (o \ s"${prefix}asset_code").extract[String]

      def assetIssuer = KeyPair.fromAccountId((o \ s"$prefix$issuerKey").extract[String])

      (o \ s"${prefix}asset_type").extract[String] match {
        case "native" => NativeAsset
        case "credit_alphanum4" => IssuedAsset4(assetCode, assetIssuer)
        case "credit_alphanum12" => IssuedAsset12(assetCode, assetIssuer)
        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
      }
    }

    def amount(prefix: String = "") = {
      val units = Amount.toBaseUnits(doubleFromString(s"${prefix}amount")).get
      asset(prefix) match {
        case nna: NonNativeAsset => IssuedAmount(units, nna)
        case NativeAsset => NativeAmount(units)
      }
    }

    Trade(
      id = (o \ "id").extract[String],
      ledgerCloseTime = date("ledger_close_time"),
      offerId = (o \ "offer_id").extract[String].toLong,
      baseAccount = account("base_account"),
      baseAmount = amount("base_"),
      counterAccount = account("counter_account"),
      counterAmount = amount("counter_"),
      baseIsSeller = (o \ "base_is_seller").extract[Boolean]
    )

}, PartialFunction.empty)
)

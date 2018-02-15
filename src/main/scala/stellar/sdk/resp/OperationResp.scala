package stellar.sdk.resp

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk._

// todo - merge OperationResps with stellar.sdk.op._
sealed trait OperationResp {
  val id: Long
  val txnHash: String
}

case class OperationCreateAccount(id: Long, txnHash: String, account: PublicKeyOps, funder: PublicKeyOps, startingBalance: NativeAmount, createdAt: ZonedDateTime) extends OperationResp


object OperationRespDeserializer extends CustomSerializer[OperationResp](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

    def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])
//
//    def asset(prefix: String = "", issuerKey: String = "asset_issuer") = {
//      def assetCode = (o \ s"${prefix}asset_code").extract[String]
//      def assetIssuer = KeyPair.fromAccountId((o \ s"$prefix$issuerKey").extract[String])
//      (o \ s"${prefix}asset_type").extract[String] match {
//        case "native" => NativeAsset
//        case "credit_alphanum4" => AssetTypeCreditAlphaNum4(assetCode, assetIssuer)
//        case "credit_alphanum12" => AssetTypeCreditAlphaNum12(assetCode, assetIssuer)
//        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
//      }
//    }
//
    def doubleFromString(key: String) = (o \ key).extract[String].toDouble

    def amount(key: String = "") = {
      val units = Amount.toBaseUnits(doubleFromString(key)).get
//      asset(prefix) match {
//        case nna: NonNativeAsset => IssuedAmount(units, nna)
//        case NativeAsset => NativeAmount(units)
//      }
      NativeAmount(units)
    }

    def date(key: String) = ZonedDateTime.from(formatter.parse((o \ key).extract[String]))
//
//    def weight = (o \ "weight").extract[Int].toShort

    val id = (o \ "id").extract[String].toLong
    val txnHash = (o \ "transaction_hash").extract[String]
    (o \ "type").extract[String] match {
      case "create_account" =>
        OperationCreateAccount(id, txnHash, account(), account("funder"), amount("starting_balance"), date("created_at"))


//      case "account_created" =>
//        val startingBalance = Amount.lumens((o \ "starting_balance").extract[String].toDouble).get
//        EffectAccountCreated(id, account(), startingBalance)
//      case "account_credited" => EffectAccountCredited(id, account(), amount())
//      case "account_debited" => EffectAccountDebited(id, account(), amount())
//      case "account_removed" => EffectAccountRemoved(id, account())
//      case "account_thresholds_updated" =>
//        val thresholds = Thresholds(
//          (o \ "low_threshold").extract[Int],
//          (o \ "med_threshold").extract[Int],
//          (o \ "high_threshold").extract[Int]
//        )
//        EffectAccountThresholdsUpdated(id, account(), thresholds)
//      case "account_home_domain_updated" => EffectAccountHomeDomainUpdated(id, account(), (o \ "home_domain").extract[String])
//      case "account_flags_updated" => EffectAccountFlagsUpdated(id, account(), (o \ "auth_required_flag").extract[Boolean])
//      case "signer_created" => EffectSignerCreated(id, account(), weight, (o \ "public_key").extract[String])
//      case "signer_updated" => EffectSignerUpdated(id, account(), weight, (o \ "public_key").extract[String])
//      case "signer_removed" => EffectSignerRemoved(id, account(), (o \ "public_key").extract[String])
//      case "trustline_created" => EffectTrustLineCreated(id, account(), asset().asInstanceOf[NonNativeAsset], doubleFromString("limit"))
//      case "trustline_updated" => EffectTrustLineUpdated(id, account(), asset().asInstanceOf[NonNativeAsset], doubleFromString("limit"))
//      case "trustline_removed" => EffectTrustLineRemoved(id, account(), asset().asInstanceOf[NonNativeAsset])
//      case "trustline_authorized" => EffectTrustLineAuthorized(id, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
//      case "trustline_deauthorized" => EffectTrustLineDeauthorized(id, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
//      case "trade" => EffectTrade(id, (o \ "offer_id").extract[Long], account(), amount("bought_"), account("seller"), amount("sold_"))
      case t => throw new RuntimeException(s"Unrecognised operation type '$t'")
    }
}, PartialFunction.empty)
)

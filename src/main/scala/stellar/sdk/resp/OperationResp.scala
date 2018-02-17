package stellar.sdk.resp

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import org.json4s.JsonAST.{JArray, JObject, JValue}
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk._

// todo - merge OperationResps with stellar.sdk.op._
sealed trait OperationResp {
  val id: Long
  val txnHash: String
  val sourceAccount: PublicKeyOps
  val createdAt: ZonedDateTime
}

// todo - delete
case class OperationCreateAccount(id: Long, txnHash: String, sourceAccount: PublicKeyOps, createdAt: ZonedDateTime,
                                  account: PublicKeyOps, funder: PublicKeyOps, startingBalance: NativeAmount) extends OperationResp

// todo - delete
case class OperationPayment(id: Long, txnHash: String, sourceAccount: PublicKeyOps, createdAt: ZonedDateTime,
                            amount: Amount, fromAccount: PublicKeyOps, toAccount: PublicKeyOps) extends OperationResp

// todo - delete
case class OperationPathPayment(id: Long, txnHash: String, sourceAccount: PublicKeyOps, createdAt: ZonedDateTime,
                                fromMaxAmount: Amount, fromAccount: PublicKeyOps, toAmount: Amount, toAccount: PublicKeyOps,
                                path: Seq[Asset]) extends OperationResp

object OperationRespDeserializer extends CustomSerializer[OperationResp](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

    def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

    def asset(prefix: String = "", obj: JValue = o) = {
      def assetCode = (obj \ s"${prefix}asset_code").extract[String]
      def assetIssuer = KeyPair.fromAccountId((obj \ s"${prefix}asset_issuer").extract[String])
      (obj \ s"${prefix}asset_type").extract[String] match {
        case "native" => NativeAsset
        case "credit_alphanum4" => AssetTypeCreditAlphaNum4(assetCode, assetIssuer)
        case "credit_alphanum12" => AssetTypeCreditAlphaNum12(assetCode, assetIssuer)
        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
      }
    }

    def doubleFromString(key: String) = (o \ key).extract[String].toDouble

    def nativeAmount(key: String) = {
      NativeAmount(Amount.toBaseUnits(doubleFromString(key)).get)
    }

    def amount(label: String = "amount", assetPrefix: String = "") = {
      val units = Amount.toBaseUnits(doubleFromString(label)).get
      asset(assetPrefix) match {
        case nna: NonNativeAsset => IssuedAmount(units, nna)
        case NativeAsset => NativeAmount(units)
      }
    }

    def date(key: String) = ZonedDateTime.from(formatter.parse((o \ key).extract[String]))
//
//    def weight = (o \ "weight").extract[Int].toShort

    val id = (o \ "id").extract[String].toLong
    val txnHash = (o \ "transaction_hash").extract[String]
    val source = account("source_account")
    val createdAt = date("created_at")
    (o \ "type").extract[String] match {
      case "create_account" =>
        OperationCreateAccount(id, txnHash, source, createdAt, account(), account("funder"), nativeAmount("starting_balance"))
      case "payment" =>
        OperationPayment(id, txnHash, source, createdAt, amount(), account("from"), account("to"))
      case "path_payment" =>
        val JArray(pathJs) = o \ "path"
        val path: List[Asset] = pathJs.map(a => asset(obj = a))
        OperationPathPayment(id, txnHash, source, createdAt, amount("source_max", "source_"), account("from"), amount(),
          account("to"), path)
      case "manage_offer" =>
        import org.json4s.native.JsonMethods._
        val doc = pretty(render(o))
        println(doc)
        null



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
      case t =>
        // throw new RuntimeException(s"Unrecognised operation type '$t'")
        null
    }
}, PartialFunction.empty)
)

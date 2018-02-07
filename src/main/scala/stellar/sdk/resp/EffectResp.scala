package stellar.sdk.resp

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk._

sealed trait EffectResp {
  val id: String
}

case class EffectAccountCreated(id: String, account: PublicKeyOps, startingBalance: NativeAmount) extends EffectResp
case class EffectAccountCredited(id: String, account: PublicKeyOps, amount: Amount) extends EffectResp
case class EffectAccountDebited(id: String, account: PublicKeyOps, amount: Amount) extends EffectResp
case class EffectAccountRemoved(id: String, account: PublicKeyOps) extends EffectResp
case class EffectAccountThresholdsUpdated(id: String, account: PublicKeyOps, thresholds: Thresholds) extends EffectResp
case class EffectAccountHomeDomainUpdated(id: String, account: PublicKeyOps, domain: String) extends EffectResp
case class EffectAccountFlagsUpdated(id: String, account: PublicKeyOps, authRequiredFlag: Boolean) extends EffectResp

class EffectRespDeserializer extends CustomSerializer[EffectResp](format => ({
  case o: JObject =>
    implicit val formats = DefaultFormats
    def account = KeyPair.fromAccountId((o \ "account").extract[String])
    def amount = {
      val units = Amount.toBaseUnits((o \ "amount").extract[String].toDouble).get
      def assetCode = (o \ "asset_code").extract[String]
      def assetIssuer = KeyPair.fromAccountId((o \ "asset_issuer").extract[String])
      (o \ "asset_type").extract[String] match {
        case "native" => NativeAmount(units)
        case "credit_alphanum4" => IssuedAmount(units, AssetTypeCreditAlphaNum4(assetCode, assetIssuer))
        case "credit_alphanum12" => IssuedAmount(units, AssetTypeCreditAlphaNum12(assetCode, assetIssuer))
        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
      }
    }
    val id = (o \ "id").extract[String]
    (o \ "type").extract[String] match {
      case "account_created" =>
        val startingBalance = Amount.lumens((o \ "starting_balance").extract[String].toDouble).get
        EffectAccountCreated(id, account, startingBalance)
      case "account_credited" => EffectAccountCredited(id, account, amount)
      case "account_debited" => EffectAccountDebited(id, account, amount)
      case "account_removed" => EffectAccountRemoved(id, account)
      case "account_thresholds_updated" =>
        val thresholds = Thresholds(
          (o \ "low_threshold").extract[Int],
          (o \ "med_threshold").extract[Int],
          (o \ "high_threshold").extract[Int]
        )
        EffectAccountThresholdsUpdated(id, account, thresholds)
      case "account_home_domain_updated" => EffectAccountHomeDomainUpdated(id, account, (o \ "home_domain").extract[String])
      case "account_flags_updated" => EffectAccountFlagsUpdated(id, account, (o \ "auth_required_flag").extract[Boolean])
      case t => throw new RuntimeException(s"Unrecognised effect type '$t'")
    }
}, PartialFunction.empty)
)

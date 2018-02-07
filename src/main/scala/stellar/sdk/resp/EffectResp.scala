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
case class EffectSignerCreated(id: String, weight: Short, publicKey: String) extends EffectResp
case class EffectSignerUpdated(id: String, weight: Short, publicKey: String) extends EffectResp
case class EffectSignerRemoved(id: String, publicKey: String) extends EffectResp
case class EffectTrustLineCreated(id: String, accn: PublicKeyOps, asset: NonNativeAsset, limit: Double) extends EffectResp
case class EffectTrustLineUpdated(id: String, accn: PublicKeyOps, asset: NonNativeAsset, limit: Double) extends EffectResp
case class EffectTrustLineRemoved(id: String, accn: PublicKeyOps, asset: NonNativeAsset) extends EffectResp

class EffectRespDeserializer extends CustomSerializer[EffectResp](format => ({
  case o: JObject =>
    implicit val formats = DefaultFormats
    def account = KeyPair.fromAccountId((o \ "account").extract[String])
    def asset = {
      def assetCode = (o \ "asset_code").extract[String]
      def assetIssuer = KeyPair.fromAccountId((o \ "asset_issuer").extract[String])
      (o \ "asset_type").extract[String] match {
        case "native" => AssetTypeNative
        case "credit_alphanum4" => AssetTypeCreditAlphaNum4(assetCode, assetIssuer)
        case "credit_alphanum12" => AssetTypeCreditAlphaNum12(assetCode, assetIssuer)
        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
      }
    }
    def doubleFromString(key: String) = (o \ key).extract[String].toDouble
    def amount = {
      val units = Amount.toBaseUnits(doubleFromString("amount")).get
      asset match {
        case nna: NonNativeAsset => IssuedAmount(units, nna)
        case AssetTypeNative => NativeAmount(units)
      }
    }
    def weight = (o \ "weight").extract[Int].toShort
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
      case "signer_created" => EffectSignerCreated(id, weight, (o \ "public_key").extract[String])
      case "signer_updated" => EffectSignerUpdated(id, weight, (o \ "public_key").extract[String])
      case "signer_removed" => EffectSignerRemoved(id, (o \ "public_key").extract[String])
      case "trustline_created" => EffectTrustLineCreated(id, account, asset.asInstanceOf[NonNativeAsset], doubleFromString("limit"))
      case "trustline_updated" => EffectTrustLineUpdated(id, account, asset.asInstanceOf[NonNativeAsset], doubleFromString("limit"))
      case "trustline_removed" => EffectTrustLineRemoved(id, account, asset.asInstanceOf[NonNativeAsset])
      case t => throw new RuntimeException(s"Unrecognised effect type '$t'")
    }
}, PartialFunction.empty)
)

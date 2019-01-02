package stellar.sdk.model.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods._
import stellar.sdk._

sealed trait EffectResponse {
  val id: String
}

case class EffectAccountCreated(id: String, account: PublicKeyOps, startingBalance: NativeAmount) extends EffectResponse

case class EffectAccountCredited(id: String, account: PublicKeyOps, amount: Amount) extends EffectResponse

case class EffectAccountDebited(id: String, account: PublicKeyOps, amount: Amount) extends EffectResponse

case class EffectAccountInflationDestinationUpdated(id: String, account: PublicKeyOps) extends EffectResponse

case class EffectAccountRemoved(id: String, account: PublicKeyOps) extends EffectResponse

case class EffectAccountThresholdsUpdated(id: String, account: PublicKeyOps, thresholds: Thresholds) extends EffectResponse

case class EffectAccountHomeDomainUpdated(id: String, account: PublicKeyOps, domain: String) extends EffectResponse

case class EffectAccountFlagsUpdated(id: String, account: PublicKeyOps) extends EffectResponse

case class EffectDataCreated(id: String, account: PublicKeyOps) extends EffectResponse

case class EffectDataRemoved(id: String, account: PublicKeyOps) extends EffectResponse

case class EffectDataUpdated(id: String, account: PublicKeyOps) extends EffectResponse

case class EffectSequenceBumped(id: String, account: PublicKeyOps, newSeq: Long) extends EffectResponse

case class EffectSignerCreated(id: String, account: PublicKeyOps, weight: Short, publicKey: String) extends EffectResponse

case class EffectSignerUpdated(id: String, account: PublicKeyOps, weight: Short, publicKey: String) extends EffectResponse

case class EffectSignerRemoved(id: String, account: PublicKeyOps, publicKey: String) extends EffectResponse

case class EffectTrustLineCreated(id: String, account: PublicKeyOps, asset: NonNativeAsset, limit: Double) extends EffectResponse

case class EffectTrustLineUpdated(id: String, account: PublicKeyOps, asset: NonNativeAsset, limit: Double) extends EffectResponse

case class EffectTrustLineRemoved(id: String, account: PublicKeyOps, asset: NonNativeAsset) extends EffectResponse

case class EffectTrustLineAuthorized(id: String, trustor: PublicKeyOps, asset: NonNativeAsset) extends EffectResponse

case class EffectTrustLineDeauthorized(id: String, trustor: PublicKeyOps, asset: NonNativeAsset) extends EffectResponse

case class EffectTrade(id: String, offerId: Long, buyer: PublicKeyOps, bought: Amount, seller: PublicKeyOps, sold: Amount) extends EffectResponse

object EffectResponseDeserializer extends ResponseParser[EffectResponse]({ o: JObject =>
  implicit val formats = DefaultFormats

  def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

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

  def doubleFromString(key: String) = (o \ key).extract[String].toDouble

  def amount(prefix: String = "") = {
    val units = Amount.toBaseUnits(doubleFromString(s"${prefix}amount")).get
    asset(prefix) match {
      case nna: NonNativeAsset => IssuedAmount(units, nna)
      case NativeAsset => NativeAmount(units)
    }
  }

  def weight = (o \ "weight").extract[Int].toShort

  val id = (o \ "id").extract[String]
  (o \ "type").extract[String] match {
    case "account_created" =>
      val startingBalance = Amount.lumens((o \ "starting_balance").extract[String].toDouble)
      EffectAccountCreated(id, account(), startingBalance)
    case "account_credited" => EffectAccountCredited(id, account(), amount())
    case "account_debited" => EffectAccountDebited(id, account(), amount())
    case "account_inflation_destination_updated" => EffectAccountInflationDestinationUpdated(id, account())
    case "account_removed" => EffectAccountRemoved(id, account())
    case "account_thresholds_updated" =>
      val thresholds = Thresholds(
        (o \ "low_threshold").extract[Int],
        (o \ "med_threshold").extract[Int],
        (o \ "high_threshold").extract[Int]
      )
      EffectAccountThresholdsUpdated(id, account(), thresholds)
    case "account_home_domain_updated" => EffectAccountHomeDomainUpdated(id, account(), (o \ "home_domain").extract[String])
    case "account_flags_updated" => EffectAccountFlagsUpdated(id, account())
    case "data_created" => EffectDataCreated(id, account())
    case "data_removed" => EffectDataRemoved(id, account())
    case "data_updated" => EffectDataUpdated(id, account())
    case "sequence_bumped" => EffectSequenceBumped(id, account(), (o \ "new_seq").extract[String].toLong)
    case "signer_created" => EffectSignerCreated(id, account(), weight, (o \ "public_key").extract[String])
    case "signer_updated" => EffectSignerUpdated(id, account(), weight, (o \ "public_key").extract[String])
    case "signer_removed" => EffectSignerRemoved(id, account(), (o \ "public_key").extract[String])
    case "trustline_created" => EffectTrustLineCreated(id, account(), asset().asInstanceOf[NonNativeAsset], doubleFromString("limit"))
    case "trustline_updated" => EffectTrustLineUpdated(id, account(), asset().asInstanceOf[NonNativeAsset], doubleFromString("limit"))
    case "trustline_removed" => EffectTrustLineRemoved(id, account(), asset().asInstanceOf[NonNativeAsset])
    case "trustline_authorized" => EffectTrustLineAuthorized(id, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
    case "trustline_deauthorized" => EffectTrustLineDeauthorized(id, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
    case "trade" => EffectTrade(id, (o \ "offer_id").extract[Long], account(), amount("bought_"), account("seller"), amount("sold_"))
    case t => throw new RuntimeException(s"Unrecognised effect type '$t': ${compact(render(o))}")
  }
})

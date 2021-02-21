package stellar.sdk.model.response

import java.time.ZonedDateTime

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods
import org.json4s.native.JsonMethods._
import stellar.sdk._
import stellar.sdk.model._

sealed trait EffectResponse {
  val id: String
  val createdAt: ZonedDateTime
}

case class EffectAccountCreated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, startingBalance: NativeAmount) extends EffectResponse

case class EffectAccountCredited(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, amount: Amount) extends EffectResponse

case class EffectAccountDebited(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, amount: Amount) extends EffectResponse

case class EffectAccountInflationDestinationUpdated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps) extends EffectResponse

case class EffectAccountRemoved(id: String, createdAt: ZonedDateTime, account: PublicKeyOps) extends EffectResponse

case class EffectAccountSponsorshipCreated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps) extends EffectResponse

case class EffectAccountThresholdsUpdated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, thresholds: Thresholds) extends EffectResponse

case class EffectAccountHomeDomainUpdated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, domain: String) extends EffectResponse

case class EffectAccountFlagsUpdated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps) extends EffectResponse

case class EffectDataCreated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps) extends EffectResponse

case class EffectDataRemoved(id: String, createdAt: ZonedDateTime, account: PublicKeyOps) extends EffectResponse

case class EffectDataUpdated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps) extends EffectResponse

case class EffectSequenceBumped(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, newSeq: Long) extends EffectResponse

case class EffectSignerCreated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, weight: Short, publicKey: String) extends EffectResponse

case class EffectSignerUpdated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, weight: Short, publicKey: String) extends EffectResponse

case class EffectSignerRemoved(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, publicKey: String) extends EffectResponse

case class EffectSignerSponsorshipCreated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, signer: PublicKeyOps, newSponsor: PublicKeyOps) extends EffectResponse

case class EffectSignerSponsorshipRemoved(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, signer: PublicKeyOps, formerSponsor: PublicKeyOps) extends EffectResponse

case class EffectSignerSponsorshipUpdated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, signer: PublicKeyOps, formerSponsor: PublicKeyOps, newSponsor: PublicKeyOps) extends EffectResponse

case class EffectTrustLineCreated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, limit: IssuedAmount) extends EffectResponse {
  val asset: NonNativeAsset = limit.asset
}

case class EffectTrustLineUpdated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, limit: IssuedAmount) extends EffectResponse {
  val asset: NonNativeAsset = limit.asset
}

case class EffectTrustLineRemoved(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, asset: NonNativeAsset) extends EffectResponse

case class EffectTrustLineAuthorized(id: String, createdAt: ZonedDateTime, trustor: PublicKeyOps, asset: NonNativeAsset) extends EffectResponse

case class EffectTrustLineAuthorizedToMaintainLiabilities(id: String, createdAt: ZonedDateTime, trustor: PublicKeyOps, asset: NonNativeAsset) extends EffectResponse

case class EffectTrustLineDeauthorized(id: String, createdAt: ZonedDateTime, trustor: PublicKeyOps, asset: NonNativeAsset) extends EffectResponse

case class EffectTrustLineSponsorshipCreated(id: String, createdAt: ZonedDateTime, account: PublicKeyOps, asset: NonNativeAsset, sponsor: PublicKeyOps) extends EffectResponse

case class EffectTrade(id: String, createdAt: ZonedDateTime, offerId: Long, buyer: PublicKeyOps, bought: Amount, seller: PublicKeyOps, sold: Amount) extends EffectResponse

case class UnrecognisedEffect(id: String, createdAt: ZonedDateTime, json: String) extends EffectResponse

object EffectResponseDeserializer extends ResponseParser[EffectResponse]({ o: JObject =>
  implicit val formats = DefaultFormats

  def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

  def asset(prefix: String = "", issuerKey: String = "asset_issuer"): Asset = {
    def assetCode = (o \ s"${prefix}asset_code").extract[String]

    def assetIssuer = KeyPair.fromAccountId((o \ s"$prefix$issuerKey").extract[String])

    (o \ s"${prefix}asset_type").extract[String] match {
      case "native" => NativeAsset
      case "credit_alphanum4" => IssuedAsset4(assetCode, assetIssuer)
      case "credit_alphanum12" => IssuedAsset12(assetCode, assetIssuer)
      case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
    }
  }

  def issuedAsset(code: String = "asset"): NonNativeAsset = Asset.parseIssuedAsset((o \ code).extract[String])

  def bigDecimal(key: String) = BigDecimal((o \ key).extract[String])

  def amount(prefix: String = "", key: String = "amount") = {
    val units = Amount.toBaseUnits(bigDecimal(s"$prefix$key")).get
    asset(prefix) match {
      case nna: NonNativeAsset => IssuedAmount(units, nna)
      case NativeAsset => NativeAmount(units)
    }
  }

  def weight = (o \ "weight").extract[Int].toShort

  val id = (o \ "id").extract[String]
  val createdAt = ZonedDateTime.parse((o \ "created_at").extract[String])
  (o \ "type").extract[String] match {
    case "account_created" =>
      val startingBalance = Amount.lumens((o \ "starting_balance").extract[String].toDouble)
      EffectAccountCreated(id, createdAt, account(), startingBalance)
    case "account_credited" => EffectAccountCredited(id, createdAt, account(), amount())
    case "account_debited" => EffectAccountDebited(id, createdAt, account(), amount())
    case "account_flags_updated" => EffectAccountFlagsUpdated(id, createdAt, account())
    case "account_home_domain_updated" => EffectAccountHomeDomainUpdated(id, createdAt, account(), (o \ "home_domain").extract[String])
    case "account_inflation_destination_updated" => EffectAccountInflationDestinationUpdated(id, createdAt, account())
    case "account_removed" => EffectAccountRemoved(id, createdAt, account())
    case "account_sponsorship_created" => EffectAccountSponsorshipCreated(id, createdAt, account())
    case "account_sponsorship_removed" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "account_sponsorship_updated" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "account_thresholds_updated" =>
      val thresholds = Thresholds(
        (o \ "low_threshold").extract[Int],
        (o \ "med_threshold").extract[Int],
        (o \ "high_threshold").extract[Int]
      )
      EffectAccountThresholdsUpdated(id, createdAt, account(), thresholds)
    case "claimable_balance_claimant_created" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "claimable_balance_claimed" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "claimable_balance_created" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "claimable_balance_sponsorship_created" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "claimable_balance_sponsorship_removed" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "claimable_balance_sponsorship_updated" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "data_created" => EffectDataCreated(id, createdAt, account())
    case "data_removed" => EffectDataRemoved(id, createdAt, account())
    case "data_sponsorship_created" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "data_sponsorship_removed" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "data_sponsorship_updated" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "data_updated" => EffectDataUpdated(id, createdAt, account())
    case "offer_created" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "offer_removed" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "offer_updated" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "sequence_bumped" => EffectSequenceBumped(id, createdAt, account(), (o \ "new_seq").extract[String].toLong)
    case "signer_created" => EffectSignerCreated(id, createdAt, account(), weight, (o \ "public_key").extract[String])
    case "signer_removed" => EffectSignerRemoved(id, createdAt, account(), (o \ "public_key").extract[String])
    case "signer_sponsorship_created" => EffectSignerSponsorshipCreated(id, createdAt, account(),
      signer = account("signer"),
      newSponsor = account("sponsor")
    )
    case "signer_sponsorship_removed" => EffectSignerSponsorshipRemoved(id, createdAt, account(),
      signer = account("signer"),
      formerSponsor = account("former_sponsor")
    )
    case "signer_sponsorship_updated" => EffectSignerSponsorshipUpdated(id, createdAt, account(),
      signer = account("signer"),
      formerSponsor = account("former_sponsor"),
      newSponsor = account("new_sponsor")
    )
    case "signer_updated" => EffectSignerUpdated(id, createdAt, account(), weight, (o \ "public_key").extract[String])
    case "trade" => EffectTrade(id, createdAt, (o \ "offer_id").extract[String].toLong, account(), amount("bought_"), account("seller"), amount("sold_"))
    case "trustline_authorized" => EffectTrustLineAuthorized(id, createdAt, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
    case "trustline_authorized_to_maintain_liabilities" => EffectTrustLineAuthorizedToMaintainLiabilities(id, createdAt, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
    case "trustline_created" => EffectTrustLineCreated(id, createdAt, account(), amount(key = "limit").asInstanceOf[IssuedAmount])
    case "trustline_deauthorized" => EffectTrustLineDeauthorized(id, createdAt, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
    case "trustline_removed" => EffectTrustLineRemoved(id, createdAt, account(), asset().asInstanceOf[NonNativeAsset])
    case "trustline_sponsorship_created" => EffectTrustLineSponsorshipCreated(id, createdAt, account(), issuedAsset(), account("sponsor"))
    case "trustline_sponsorship_removed" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "trustline_sponsorship_updated" => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
    case "trustline_updated" => EffectTrustLineUpdated(id, createdAt, account(), amount(key = "limit").asInstanceOf[IssuedAmount])
    case _ => UnrecognisedEffect(id, createdAt, JsonMethods.compact(JsonMethods.render(o)))
  }
})

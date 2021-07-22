package stellar.sdk.model.response

import java.time.ZonedDateTime
import java.util.Locale

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model._
import stellar.sdk.model.op.JsonSnippets

class AccountEffectResponseSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "a create account effect document" should {
    "parse to a create account effect" >> prop { (id: String, created: ZonedDateTime, accn: AccountId, amount: NativeAmount) =>
      parse(doc(id, created, accn, "account_created", "starting_balance" -> amountString(amount)))
        .extract[EffectResponse] mustEqual EffectAccountCreated(id, created, accn, amount)
    }.setGen1(Gen.identifier)
  }

  "a debit account effect document" should {
    "parse to a debit account effect with native amount" >> prop { (id: String, created: ZonedDateTime, accn: AccountId, amount: NativeAmount) =>
      val json = doc(id, created, accn, "account_debited",
        "asset_type" -> "native",
        "amount" -> amountString(amount))
      parse(json).extract[EffectResponse] mustEqual EffectAccountDebited(id, created, accn, amount)
    }.setGen1(Gen.identifier)

    "parse to a debit account effect with non-native amount" >> prop { (id: String, created: ZonedDateTime, accn: AccountId, amount: IssuedAmount) =>
      val json = doc(id, created, accn, "account_debited",
        "asset_type" -> (amount.asset match {
          case _: IssuedAsset4 => "credit_alphanum4"
          case _ => "credit_alphanum12"
        }),
        "asset_code" -> amount.asset.code,
        "asset_issuer" -> amount.asset.issuer.accountId,
        "amount" -> amountString(amount))
      parse(json).extract[EffectResponse] mustEqual EffectAccountDebited(id, created, accn, amount)
    }.setGen1(Gen.identifier)
  }

  "a credit account effect document" should {
    "parse to a credit account effect with native amount" >> prop { (id: String, created: ZonedDateTime, accn: AccountId, amount: NativeAmount) =>
      val json = doc(id, created, accn, "account_credited",
        "asset_type" -> "native",
        "amount" -> amountString(amount))
      parse(json).extract[EffectResponse] mustEqual EffectAccountCredited(id, created, accn, amount)
    }.setGen1(Gen.identifier)

    "parse to a credit account effect with non-native amount" >> prop { (id: String, created: ZonedDateTime, accn: AccountId, amount: IssuedAmount) =>
      val json = doc(id, created, accn, "account_credited",
        "asset_type" -> (amount.asset match {
          case _: IssuedAsset4 => "credit_alphanum4"
          case _ => "credit_alphanum12"
        }),
        "asset_code" -> amount.asset.code,
        "asset_issuer" -> amount.asset.issuer.accountId,
        "amount" -> amountString(amount))
      parse(json).extract[EffectResponse] mustEqual EffectAccountCredited(id, created, accn, amount)
    }.setGen1(Gen.identifier)
  }

  "an account removed effect document" should {
    "parse to an account removed effect" >> prop { (id: String, created: ZonedDateTime, accn: AccountId) =>
      val json = doc(id, created, accn, "account_removed")
      parse(json).extract[EffectResponse] mustEqual EffectAccountRemoved(id, created, accn)
    }.setGen1(Gen.identifier)
  }

  "an account inflation destination update effect document" should {
    "parse to an effect" >> prop { (id: String, created: ZonedDateTime, accn: AccountId) =>
      val json = doc(id, created, accn, "account_inflation_destination_updated")
      parse(json).extract[EffectResponse] mustEqual EffectAccountInflationDestinationUpdated(id, created, accn)
    }.setGen1(Gen.identifier)
  }

  "an account thresholds updated effect document" should {
    "parse to an account thresholds updated effect" >> prop { (id: String, created: ZonedDateTime, accn: AccountId, thresholds: Thresholds) =>
      val json = doc(id, created, accn, "account_thresholds_updated",
        "low_threshold" -> thresholds.low,
        "med_threshold" -> thresholds.med,
        "high_threshold" -> thresholds.high)
      parse(json).extract[EffectResponse] mustEqual EffectAccountThresholdsUpdated(id, created, accn, thresholds)
    }.setGen1(Gen.identifier)
  }

  "an account home domain updated effect document" should {
    "parse to an account home domain updated effect" >> prop { (id: String, created: ZonedDateTime, accn: AccountId, domain: String) =>
      val json = doc(id, created, accn, "account_home_domain_updated",
        "home_domain" -> domain)
      parse(json).extract[EffectResponse] mustEqual EffectAccountHomeDomainUpdated(id, created, accn, domain)
    }.setGen1(Gen.identifier).setGen4(Gen.identifier)
  }

  "an account flags updated effect document" should {
    "parse to an account flags updated effect" >> prop { (id: String, created: ZonedDateTime, accn: AccountId) =>
      val json = doc(id, created, accn, "account_flags_updated")
      parse(json).extract[EffectResponse] mustEqual EffectAccountFlagsUpdated(id, created, accn)
    }.setGen1(Gen.identifier)
  }

  "an account sponsorship created effect document" should {
    "parse from json" >> prop { (id: String, created: ZonedDateTime, accn: AccountId) =>
      val json = doc(id, created, accn, "account_sponsorship_created")
      parse(json).extract[EffectResponse] mustEqual EffectAccountSponsorshipCreated(id, created, accn)
    }.setGen1(Gen.identifier)
  }

  def doc(id: String, created: ZonedDateTime, accnId: AccountId, tpe: String, extra: (String, Any)*) =
    s"""
       |{
       |  "id": "$id",
       |  "account": "${accnId.publicKey.accountId}",
       |  ${accnId.subAccountId.map(id => s""""account_muxed_id": "$id",""").getOrElse("")}
       |  "created_at": "${formatter.format(created)}",
       |  "type": "$tpe",
       |  "type_i": 3,
       |  ${
      extra.map {
        case (k, v: String) => s""""$k": "$v"""".trim
        case (k, v) => s""""$k": $v""".trim
      }.mkString(", ")
    }
       |}
    """.stripMargin

}

/*
  "a signer created effect document" should {
    "parse to a signer created effect" >> prop { (id : String, weight: Byte, pubKey: String) =>
      val json = doc(id, created, accn, "account_flags_updated", "auth_required_flag" -> authRequired)
      parse(json).extract[EffectResp] mustEqual EffectSignerUpdated(id, weight, pubKey)
    }.setGen1(Gen.identifier)
  }
 */

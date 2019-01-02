package stellar.sdk.model.response

import java.util.Locale

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._

class AccountEffectResponseSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "a create account effect document" should {
    "parse to a create account effect" >> prop { (id: String, accn: KeyPair, amount: NativeAmount) =>
      parse(doc(id, accn, "account_created", "starting_balance" -> amountString(amount)))
        .extract[EffectResponse] mustEqual EffectAccountCreated(id, accn.asPublicKey, amount)
    }.setGen1(Gen.identifier)
  }

  "a debit account effect document" should {
    "parse to a debit account effect with native amount" >> prop { (id: String, accn: KeyPair, amount: NativeAmount) =>
      val json = doc(id, accn, "account_debited",
        "asset_type" -> "native",
        "amount" -> amountString(amount))
      parse(json).extract[EffectResponse] mustEqual EffectAccountDebited(id, accn.asPublicKey, amount)
    }.setGen1(Gen.identifier)

    "parse to a debit account effect with non-native amount" >> prop { (id: String, accn: KeyPair, amount: IssuedAmount) =>
      val json = doc(id, accn, "account_debited",
        "asset_type" -> (amount.asset match {
          case _: IssuedAsset4 => "credit_alphanum4"
          case _ => "credit_alphanum12"
        }),
        "asset_code" -> amount.asset.code,
        "asset_issuer" -> amount.asset.issuer.accountId,
        "amount" -> amountString(amount))
      parse(json).extract[EffectResponse] mustEqual EffectAccountDebited(id, accn.asPublicKey, amount)
    }.setGen1(Gen.identifier)
  }

  "a credit account effect document" should {
    "parse to a credit account effect with native amount" >> prop { (id: String, accn: KeyPair, amount: NativeAmount) =>
      val json = doc(id, accn, "account_credited",
        "asset_type" -> "native",
        "amount" -> amountString(amount))
      parse(json).extract[EffectResponse] mustEqual EffectAccountCredited(id, accn.asPublicKey, amount)
    }.setGen1(Gen.identifier)

    "parse to a credit account effect with non-native amount" >> prop { (id: String, accn: KeyPair, amount: IssuedAmount) =>
      val json = doc(id, accn, "account_credited",
        "asset_type" -> (amount.asset match {
          case _: IssuedAsset4 => "credit_alphanum4"
          case _ => "credit_alphanum12"
        }),
        "asset_code" -> amount.asset.code,
        "asset_issuer" -> amount.asset.issuer.accountId,
        "amount" -> amountString(amount))
      parse(json).extract[EffectResponse] mustEqual EffectAccountCredited(id, accn.asPublicKey, amount)
    }.setGen1(Gen.identifier)
  }

  "an account removed effect document" should {
    "parse to an account removed effect" >> prop { (id: String, accn: KeyPair) =>
      val json = doc(id, accn, "account_removed")
      parse(json).extract[EffectResponse] mustEqual EffectAccountRemoved(id, accn.asPublicKey)
    }.setGen1(Gen.identifier)
  }

  "an account inflation destination update effect document" should {
    "parse to an effect" >> prop { (id: String, accn: KeyPair) =>
      val json = doc(id, accn, "account_inflation_destination_updated")
      parse(json).extract[EffectResponse] mustEqual EffectAccountInflationDestinationUpdated(id, accn.asPublicKey)
    }.setGen1(Gen.identifier)
  }

  "an account thresholds updated effect document" should {
    "parse to an account thresholds updated effect" >> prop { (id: String, accn: KeyPair, thresholds: Thresholds) =>
      val json = doc(id, accn, "account_thresholds_updated",
        "low_threshold" -> thresholds.low,
        "med_threshold" -> thresholds.med,
        "high_threshold" -> thresholds.high)
      parse(json).extract[EffectResponse] mustEqual EffectAccountThresholdsUpdated(id, accn.asPublicKey, thresholds)
    }.setGen1(Gen.identifier)
  }

  "an account home domain updated effect document" should {
    "parse to an account home domain updated effect" >> prop { (id: String, accn: KeyPair, domain: String) =>
      val json = doc(id, accn, "account_home_domain_updated",
        "home_domain" -> domain)
      parse(json).extract[EffectResponse] mustEqual EffectAccountHomeDomainUpdated(id, accn.asPublicKey, domain)
    }.setGen1(Gen.identifier).setGen3(Gen.identifier)
  }

  "an account flags updated effect document" should {
    "parse to an account flags updated effect" >> prop { (id: String, accn: KeyPair) =>
      val json = doc(id, accn, "account_flags_updated")
      parse(json).extract[EffectResponse] mustEqual EffectAccountFlagsUpdated(id, accn.asPublicKey)
    }.setGen1(Gen.identifier)
  }

  def doc(id: String, accn: PublicKeyOps, tpe: String, extra: (String, Any)*) =
    s"""
       |{
       |  "_links": {
       |    "operation": {
       |      "href": "https://horizon-testnet.stellar.org/operations/10157597659144"
       |    },
       |    "succeeds": {
       |      "href": "https://horizon-testnet.stellar.org/effects?order=desc\u0026cursor=10157597659144-2"
       |    },
       |    "precedes": {
       |      "href": "https://horizon-testnet.stellar.org/effects?order=asc\u0026cursor=10157597659144-2"
       |    }
       |  },
       |  "id": "$id",
       |  "paging_token": "10157597659144-2",
       |  "account": "${accn.accountId}",
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

  def amountString(a: Amount): String = "%.7f".formatLocal(Locale.ROOT, a.units / math.pow(10, 7))

}

/*
  "a signer created effect document" should {
    "parse to a signer created effect" >> prop { (id : String, weight: Byte, pubKey: String) =>
      val json = doc(id, accn, "account_flags_updated", "auth_required_flag" -> authRequired)
      parse(json).extract[EffectResp] mustEqual EffectSignerUpdated(id, weight, pubKey)
    }.setGen1(Gen.identifier)
  }
 */

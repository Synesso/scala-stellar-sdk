package stellar.sdk.resp

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._

class EffectRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + new EffectRespDeserializer

  "a create account effect document" should {
    "parse to a create account effect" >> prop { (id: String, accn: KeyPair, amount: NativeAmount) =>
      parse(doc(id, accn, "account_created", "starting_balance" -> amountString(amount)))
        .extract[EffectResp] mustEqual EffectAccountCreated(id, accn.asVerifyingKey, amount)
    }.setGen1(Gen.identifier)
  }

  "a debit account effect document" should {
    "parse to a debit account effect with native amount" >> prop { (id: String, accn: KeyPair, amount: NativeAmount) =>
      val json = doc(id, accn, "account_debited",
        "asset_type" -> "native",
        "amount" -> amountString(amount))
      parse(json).extract[EffectResp] mustEqual EffectAccountDebited(id, accn.asVerifyingKey, amount)
    }.setGen1(Gen.identifier)

    "parse to a debit account effect with non-native amount" >> prop { (id: String, accn: KeyPair, amount: IssuedAmount) =>
      val json = doc(id, accn, "account_debited",
        "asset_type" -> (amount.asset match {
          case _: AssetTypeCreditAlphaNum4 => "credit_alphanum4"
          case _ => "credit_alphanum12"
        }),
        "asset_code" -> amount.asset.code,
        "asset_issuer" -> amount.asset.issuer.accountId,
        "amount" -> amountString(amount))
      parse(json).extract[EffectResp] mustEqual EffectAccountDebited(id, accn.asVerifyingKey, amount)
    }.setGen1(Gen.identifier)
  }

  def doc(id: String, accn: PublicKeyOps, tpe: String, extra: (String, String)*) =
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
      |  ${extra.map{case (k, v) => s""" "$k": "$v" """.trim}.mkString(", ")}
      |}
    """.stripMargin

  def amountString(a: Amount): String = f"${a.units / math.pow(10, 7)}%.7f"

}

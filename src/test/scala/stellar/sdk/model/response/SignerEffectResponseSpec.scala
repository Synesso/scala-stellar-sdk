package stellar.sdk.model.response

import java.time.ZonedDateTime
import java.util.Locale

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.Amount
import stellar.sdk.model.op.JsonSnippets

class SignerEffectResponseSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "a signer created effect document" should {
    "parse to a signer created effect" >> prop { (id: String, created: ZonedDateTime, kp: KeyPair, weight: Short, pubKey: String) =>
      val json = doc(id, created, kp, "signer_created", weight, "public_key" -> pubKey)
      parse(json).extract[EffectResponse] mustEqual EffectSignerCreated(id, created, kp.asPublicKey, weight, pubKey)
    }.setGen1(Gen.identifier).setGen5(Gen.identifier)
  }

  "a signer updated effect document" should {
    "parse to a signer updated effect" >> prop { (id: String, created: ZonedDateTime, kp: KeyPair, weight: Short, pubKey: String) =>
      val json = doc(id, created, kp, "signer_updated", weight, "public_key" -> pubKey)
      parse(json).extract[EffectResponse] mustEqual EffectSignerUpdated(id, created, kp.asPublicKey, weight, pubKey)
    }.setGen1(Gen.identifier).setGen5(Gen.identifier)
  }

  "a signer removed effect document" should {
    "parse to a signer removed effect" >> prop { (id: String, created: ZonedDateTime, kp: KeyPair, pubKey: String) =>
      val json = doc(id, created, kp, "signer_removed", 0, "public_key" -> pubKey)
      parse(json).extract[EffectResponse] mustEqual EffectSignerRemoved(id, created, kp.asPublicKey, pubKey)
    }.setGen1(Gen.identifier).setGen4(Gen.identifier)
  }

  def doc(id: String, created: ZonedDateTime, kp: KeyPair, tpe: String, weight: Short, extra: (String, Any)*) =
    s"""
       |{
       |  "id": "$id",
       |  "paging_token": "10157597659144-2",
       |  "account": "${kp.accountId}",
       |  "weight": $weight
       |  "created_at": "${formatter.format(created)}",
       |  "type": "$tpe",
       |  "type_i": 10,
       |  ${
      extra.map {
        case (k, v: String) => s""""$k": "$v"""".trim
        case (k, v) => s""""$k": $v""".trim
      }.mkString(", ")
    }
       |}
    """.stripMargin

}

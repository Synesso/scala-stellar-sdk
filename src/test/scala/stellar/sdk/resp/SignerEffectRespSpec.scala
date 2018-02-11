package stellar.sdk.resp

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._

class SignerEffectRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectRespDeserializer

  "a signer created effect document" should {
    "parse to a signer created effect" >> prop { (id : String, weight: Short, pubKey: String) =>
      val json = doc(id, "signer_created", weight, "public_key" -> pubKey)
      parse(json).extract[EffectResp] mustEqual EffectSignerCreated(id, weight, pubKey)
    }.setGen1(Gen.identifier).setGen3(Gen.identifier)
  }

  "a signer updated effect document" should {
    "parse to a signer updated effect" >> prop { (id : String, weight: Short, pubKey: String) =>
      val json = doc(id, "signer_updated", weight, "public_key" -> pubKey)
      parse(json).extract[EffectResp] mustEqual EffectSignerUpdated(id, weight, pubKey)
    }.setGen1(Gen.identifier).setGen3(Gen.identifier)
  }

  "a signer removed effect document" should {
    "parse to a signer removed effect" >> prop { (id : String, pubKey: String) =>
      val json = doc(id, "signer_removed", 0, "public_key" -> pubKey)
      parse(json).extract[EffectResp] mustEqual EffectSignerRemoved(id, pubKey)
    }.setGen1(Gen.identifier).setGen2(Gen.identifier)
  }

  def doc(id: String, tpe: String, weight: Short, extra: (String, Any)*) =
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
      |  "weight": $weight
      |  "type": "$tpe",
      |  "type_i": 10,
      |  ${extra.map{
             case (k, v: String) => s""""$k": "$v"""".trim
             case (k, v) => s""""$k": $v""".trim
          }.mkString(", ")}
      |}
    """.stripMargin

  def amountString(a: Amount): String = f"${a.units / math.pow(10, 7)}%.7f"

}

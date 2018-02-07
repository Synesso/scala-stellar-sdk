package stellar.sdk.resp

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._

class TrustLineEffectRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + new EffectRespDeserializer

  "a trustline created effect document" should {
    "parse to a trustline created effect" >> prop {
      (id : String, accn: KeyPair, asset: NonNativeAsset, limit: Double) =>
      val json = doc(id, "trustline_created", accn, asset, limit)
      parse(json).extract[EffectResp] mustEqual EffectTrustLineCreated(id, accn.asVerifyingKey, asset, limit)
    }.setGen1(Gen.identifier).setGen4(Gen.posNum[Double])
  }

  "a trustline updated effect document" should {
    "parse to a trustline updated effect" >> prop {
      (id : String, accn: KeyPair, asset: NonNativeAsset, limit: Double) =>
      val json = doc(id, "trustline_updated", accn, asset, limit)
      parse(json).extract[EffectResp] mustEqual EffectTrustLineUpdated(id, accn.asVerifyingKey, asset, limit)
    }.setGen1(Gen.identifier).setGen4(Gen.posNum[Double])
  }

  "a trustline removed effect document" should {
    "parse to a trustline removed effect" >> prop { (id : String, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, "trustline_removed", accn, asset, 0.0)
      parse(json).extract[EffectResp] mustEqual EffectTrustLineRemoved(id, accn.asVerifyingKey, asset)
    }.setGen1(Gen.identifier)
  }

  def doc(id: String, tpe: String, accn: PublicKeyOps, asset: NonNativeAsset, limit: Double) = {
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
      |  "type_i": 20,
      |  "asset_type": "${asset.typeString}",
      |  "asset_code": "${asset.code}",
      |  "asset_issuer": "${asset.issuer.accountId}",
      |  "limit": "$limit"
      |}
    """.stripMargin
  }


  def amountString(a: Amount): String = f"${a.units / math.pow(10, 7)}%.7f"

}

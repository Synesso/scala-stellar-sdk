package stellar.sdk.resp

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._

class TrustLineAuthEffectRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectRespDeserializer

  "an authorize trustline effect document" should {
    "parse to an authorize trustline effect" >> prop { (id : String, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, "trustline_authorized", accn, asset, 0.0)
      parse(json).extract[EffectResp] mustEqual EffectTrustLineAuthorized(id, accn.asVerifyingKey, asset)
    }.setGen1(Gen.identifier)
  }

  "a deauthorize trustline effect document" should {
    "parse to a deauthorize trustline effect" >> prop { (id : String, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, "trustline_deauthorized", accn, asset, 0.0)
      parse(json).extract[EffectResp] mustEqual EffectTrustLineDeauthorized(id, accn.asVerifyingKey, asset)
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
      |  "account": "${asset.issuer.accountId}",
      |  "type": "$tpe",
      |  "type_i": 23,
      |  "asset_type": "${asset.typeString}",
      |  "asset_code": "${asset.code}",
      |  "trustor": "${accn.accountId}"
      |}
    """.stripMargin
  }
}

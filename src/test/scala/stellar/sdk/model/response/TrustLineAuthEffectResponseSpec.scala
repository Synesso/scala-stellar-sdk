package stellar.sdk.model.response

import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.NonNativeAsset

class TrustLineAuthEffectResponseSpec extends Specification with ArbitraryInput {

  implicit val formats: Formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "an authorize trustline effect document" should {
    "parse to an authorize trustline effect" >> prop { (id: String, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, "trustline_authorized", accn, asset, 0.0)
      parse(json).extract[EffectResponse] mustEqual EffectTrustLineAuthorized(id, accn.asPublicKey, asset)
    }.setGen1(Gen.identifier)
  }

  "an authorize to maintain liabilities effect document" should {
    "parse to an authorize to maintain liabilities effect" >> prop { (id: String, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, "trustline_authorized_to_maintain_liabilities", accn, asset, 0.0)
      parse(json).extract[EffectResponse] mustEqual EffectTrustLineAuthorizedToMaintainLiabilities(id, accn.asPublicKey, asset)
    }.setGen1(Gen.identifier)
  }

  "a deauthorize trustline effect document" should {
    "parse to a deauthorize trustline effect" >> prop { (id: String, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, "trustline_deauthorized", accn, asset, 0.0)
      parse(json).extract[EffectResponse] mustEqual EffectTrustLineDeauthorized(id, accn.asPublicKey, asset)
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

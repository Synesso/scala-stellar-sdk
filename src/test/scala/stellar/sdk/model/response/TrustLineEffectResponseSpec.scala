package stellar.sdk.model.response

import java.util.Locale

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.{Amount, IssuedAmount, NonNativeAsset}

class TrustLineEffectResponseSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "a trustline created effect document" should {
    "parse to a trustline created effect" >> prop {
      (id: String, accn: KeyPair, asset: NonNativeAsset, limit: Long) =>
        val json = doc(id, "trustline_created", accn, asset, limit)
        parse(json).extract[EffectResponse] mustEqual EffectTrustLineCreated(id, accn.asPublicKey, IssuedAmount(limit, asset))
    }.setGen1(Gen.identifier).setGen4(Gen.posNum[Long])
  }

  "a trustline updated effect document" should {
    "parse to a trustline updated effect" >> prop {
      (id: String, accn: KeyPair, asset: NonNativeAsset, limit: Long) =>
        val json = doc(id, "trustline_updated", accn, asset, limit)
        parse(json).extract[EffectResponse] mustEqual EffectTrustLineUpdated(id, accn.asPublicKey, IssuedAmount(limit, asset))
    }.setGen1(Gen.identifier).setGen4(Gen.posNum[Long])
  }

  "a trustline removed effect document" should {
    "parse to a trustline removed effect" >> prop { (id: String, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, "trustline_removed", accn, asset, 0)
      parse(json).extract[EffectResponse] mustEqual EffectTrustLineRemoved(id, accn.asPublicKey, asset)
    }.setGen1(Gen.identifier)
  }

  def doc(id: String, tpe: String, accn: PublicKeyOps, asset: NonNativeAsset, limit: Long) = {
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
       |  "limit": "${limit / math.pow(10, 7)}"
       |}
    """.stripMargin
  }


  def amountString(a: Amount): String = "%.7f".formatLocal(Locale.ROOT, a.units / math.pow(10, 7))

}

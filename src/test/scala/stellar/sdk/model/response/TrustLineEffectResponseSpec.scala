package stellar.sdk.model.response

import java.time.ZonedDateTime
import java.util.Locale

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.op.JsonSnippets
import stellar.sdk.model.{Amount, IssuedAmount, NonNativeAsset}

class TrustLineEffectResponseSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "a trustline created effect document" should {
    "parse to a trustline created effect" >> prop {
      (id: String, created: ZonedDateTime, accn: KeyPair, asset: NonNativeAsset, limit: Long) =>
        val json = doc(id, created, "trustline_created", accn, asset, limit)
        parse(json).extract[EffectResponse] mustEqual EffectTrustLineCreated(id, created, accn.asPublicKey, IssuedAmount(limit, asset))
    }.setGen1(Gen.identifier).setGen5(Gen.posNum[Long])
  }

  "a trustline updated effect document" should {
    "parse to a trustline updated effect" >> prop {
      (id: String, created: ZonedDateTime, accn: KeyPair, asset: NonNativeAsset, limit: Long) =>
        val json = doc(id, created, "trustline_updated", accn, asset, limit)
        parse(json).extract[EffectResponse] mustEqual EffectTrustLineUpdated(id, created, accn.asPublicKey, IssuedAmount(limit, asset))
    }.setGen1(Gen.identifier).setGen5(Gen.posNum[Long])
  }

  "a trustline removed effect document" should {
    "parse to a trustline removed effect" >> prop { (id: String, created: ZonedDateTime, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, created, "trustline_removed", accn, asset, 0)
      parse(json).extract[EffectResponse] mustEqual EffectTrustLineRemoved(id, created, accn.asPublicKey, asset)
    }.setGen1(Gen.identifier)
  }

  def doc(id: String, created: ZonedDateTime, tpe: String, accn: PublicKeyOps, asset: NonNativeAsset, limit: Long) = {
    s"""
       |{
       |  "id": "$id",
       |  "paging_token": "10157597659144-2",
       |  "account": "${accn.accountId}",
       |  "type": "$tpe",
       |  "type_i": 20,
       |  "created_at": "${formatter.format(created)}",
       |  "asset_type": "${asset.typeString}",
       |  "asset_code": "${asset.code}",
       |  "asset_issuer": "${asset.issuer.accountId}",
       |  "limit": "${limit / math.pow(10, 7)}"
       |}
    """.stripMargin
  }
}

package stellar.sdk.model.response

import java.time.ZonedDateTime

import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.NonNativeAsset
import stellar.sdk.model.op.JsonSnippets

class TrustLineAuthEffectResponseSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats: Formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "an authorize trustline effect document" should {
    "parse to an authorize trustline effect" >> prop { (id: String, created: ZonedDateTime, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, created, "trustline_authorized", accn, asset, 0.0)
      parse(json).extract[EffectResponse] mustEqual EffectTrustLineAuthorized(id, created, accn.asPublicKey, asset)
    }.setGen1(Gen.identifier)
  }

  "an authorize to maintain liabilities effect document" should {
    "parse to an authorize to maintain liabilities effect" >> prop { (id: String, created: ZonedDateTime, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, created, "trustline_authorized_to_maintain_liabilities", accn, asset, 0.0)
      parse(json).extract[EffectResponse] mustEqual EffectTrustLineAuthorizedToMaintainLiabilities(id, created, accn.asPublicKey, asset)
    }.setGen1(Gen.identifier)
  }

  "a deauthorize trustline effect document" should {
    "parse to a deauthorize trustline effect" >> prop { (id: String, created: ZonedDateTime, accn: KeyPair, asset: NonNativeAsset) =>
      val json = doc(id, created, "trustline_deauthorized", accn, asset, 0.0)
      parse(json).extract[EffectResponse] mustEqual EffectTrustLineDeauthorized(id, created, accn.asPublicKey, asset)
    }.setGen1(Gen.identifier)
  }

  def doc(id: String, created: ZonedDateTime, tpe: String, accn: PublicKeyOps, asset: NonNativeAsset, limit: Double) = {
    s"""
       |{
       |  "id": "$id",
       |  "paging_token": "10157597659144-2",
       |  "account": "${asset.issuer.accountId}",
       |  "created_at": "${formatter.format(created)}",
       |  "type": "$tpe",
       |  "type_i": 23,
       |  "asset_type": "${asset.typeString}",
       |  "asset_code": "${asset.code}",
       |  "trustor": "${accn.accountId}"
       |}
    """.stripMargin
  }
}

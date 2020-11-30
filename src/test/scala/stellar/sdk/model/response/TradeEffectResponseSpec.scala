package stellar.sdk.model.response

import java.time.ZonedDateTime
import java.util.Locale

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.op.JsonSnippets
import stellar.sdk.model.{Amount, NonNativeAsset}

class TradeEffectResponseSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "a trade effect document" should {
    "parse to a trade effect" >> prop {
      (id: String, created: ZonedDateTime, offerId: Long, buyer: KeyPair, bought: Amount, seller: KeyPair, sold: Amount) =>
        val json = doc(id, created, offerId, buyer, bought, seller, sold)
        parse(json).extract[EffectResponse] mustEqual EffectTrade(id, created, offerId, buyer, bought, seller, sold)
    }.setGen1(Gen.identifier).setGen3(Gen.posNum[Long])
  }

  def doc(id: String, created: ZonedDateTime, offerId: Long, buyer: PublicKeyOps, bought: Amount, seller: PublicKeyOps, sold: Amount) = {
    s""" {
        "id": "$id",
        "paging_token": "31161168848490497-2",
        "account": "${buyer.accountId}",
        "type": "trade",
        "type_i": 33,
        "created_at": "${formatter.format(created)}",
        "seller": "${seller.accountId}",
        "offer_id": $offerId,
        ${amountDocPortion(sold, sold = true)},
        ${amountDocPortion(bought, sold = false)}
      }"""
  }

  def amountDocPortion(amount: Amount, sold: Boolean): String = {
    val bs = if (sold) "sold" else "bought"
    amount.asset match {
      case nn: NonNativeAsset =>
        s""""${bs}_amount": "${amountString(amount)}",
           |"${bs}_asset_type": "${nn.typeString}",
           |"${bs}_asset_code": "${nn.code}",
           |"${bs}_asset_issuer": "${nn.issuer.accountId}"
        """.stripMargin.trim

      case _ =>
        s""""${bs}_amount": "${amountString(amount)}",
           |"${bs}_asset_type": "native"
        """.stripMargin.trim
    }
  }
}

package stellar.sdk.model.response

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.op.JsonSnippets
import stellar.sdk.model.{AccountId, Amount, NonNativeAsset}

import java.time.ZonedDateTime

class TradeEffectResponseSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectResponseDeserializer

  "a trade effect document" should {
    "parse to a trade effect" >> prop {
      (id: String, created: ZonedDateTime, offerId: Long, buyer: AccountId, bought: Amount, seller: AccountId, sold: Amount) =>
        val json = doc(id, created, offerId, buyer, bought, seller, sold)
        parse(json).extract[EffectResponse] mustEqual EffectTrade(id, created, offerId, buyer, bought, seller, sold)
    }.setGen1(Gen.identifier).setGen3(Gen.posNum[Long])
  }

  def doc(id: String, created: ZonedDateTime, offerId: Long, buyer: AccountId, bought: Amount, seller: AccountId, sold: Amount) = {
    s""" {
        "id": "$id",
        "paging_token": "31161168848490497-2",
        "account": "${buyer.publicKey.accountId}",
        ${buyer.subAccountId.map(id => s""""account_muxed_id": "$id",""").getOrElse("")}
        "type": "trade",
        "type_i": 33,
        "created_at": "${formatter.format(created)}",
        "seller": "${seller.publicKey.accountId}",
        ${seller.subAccountId.map(id => s""""seller_muxed_id": "$id",""").getOrElse("")}
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

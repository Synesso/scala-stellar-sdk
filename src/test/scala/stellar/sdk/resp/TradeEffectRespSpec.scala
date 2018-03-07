package stellar.sdk.resp

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk._

class TradeEffectRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + EffectRespDeserializer

  "a trade effect document" should {
    "parse to a trade effect" >> prop {
      (id: String, offerId: Long, buyer: KeyPair, bought: Amount, seller: KeyPair, sold: Amount) =>
        val json = doc(id, offerId, buyer, bought, seller, sold)
        parse(json).extract[EffectResp] mustEqual EffectTrade(id, offerId, buyer, bought, seller, sold)
    }.setGen1(Gen.identifier).setGen2(Gen.posNum[Long])
  }

  def doc(id: String, offerId: Long, buyer: PublicKeyOps, bought: Amount, seller: PublicKeyOps, sold: Amount) = {
    s""" {
        "_links": {
          "operation": {
            "href": "https://horizon-testnet.stellar.org/operations/31161168848490497"
          },
          "succeeds": {
            "href": "https://horizon-testnet.stellar.org/effects?order=desc&cursor=31161168848490497-2"
          },
          "precedes": {
            "href": "https://horizon-testnet.stellar.org/effects?order=asc&cursor=31161168848490497-2"
          }
        },
        "id": "$id",
        "paging_token": "31161168848490497-2",
        "account": "${buyer.accountId}",
        "type": "trade",
        "type_i": 33,
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

  def amountString(a: Amount): String = f"${a.units / math.pow(10, 7)}%.7f"
}

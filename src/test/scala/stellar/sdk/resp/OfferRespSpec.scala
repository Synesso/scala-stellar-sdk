package stellar.sdk.resp

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk.{Amount, ArbitraryInput, Asset, NonNativeAsset}

class OfferRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + OfferRespDeserializer
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

  "an offer response document" should {
    "parse to an offer response" >> prop { or: OfferResp =>
      val json =
        s"""
           |{
           |  "_links": {
           |    "self": {
           |      "href": "https://horizon-testnet.stellar.org/offers/101542"
           |    },
           |    "offer_maker": {
           |      "href": "https://horizon-testnet.stellar.org/accounts/GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"
           |    }
           |  },
           |  "id": ${or.id},
           |  "paging_token": "101542",
           |  "seller": "${or.seller.accountId}",
           |  "selling": {
           |    ${assetJson(or.selling.asset)}
           |  },
           |  "buying": {
           |    ${assetJson(or.buying)}
           |  },
           |  "amount": "${amountString(or.selling)}",
           |  "price_r": {
           |    "n": ${or.price.n},
           |    "d": ${or.price.d}
           |  },
           |  "price": "3.0300000",
           |  "last_modified_ledger": ${or.lastModifiedLedger},
           |  "last_modified_time": "${formatter.format(or.lastModifiedTime)}"
           |}
           |
        """.stripMargin

      parse(json).extract[OfferResp] mustEqual or
    }
  }

  def assetJson(asset: Asset) = asset match {
    case nn: NonNativeAsset =>
      s"""
         |"asset_type": "${nn.typeString}",
         |"asset_code": "${nn.code}",
         |"asset_issuer": "${nn.issuer.accountId}"
        """.stripMargin.trim

    case _ => """"asset_type": "native""""
  }

  def amountString(a: Amount): String = f"${a.units / math.pow(10, 7)}%.7f"


}

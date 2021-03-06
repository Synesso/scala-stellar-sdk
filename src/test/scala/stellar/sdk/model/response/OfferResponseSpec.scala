package stellar.sdk.model.response

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk.model.{Amount, Asset, NonNativeAsset}
import stellar.sdk.ArbitraryInput

class OfferResponseSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + OfferRespDeserializer
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

  "an offer response document" should {
    "parse to an offer response" >> prop { or: OfferResponse =>
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
           |  ${or.sponsor.map(s => s""""sponsor": "${s.accountId}",""").getOrElse("")}
           |  "last_modified_ledger": ${or.lastModifiedLedger},
           |  "last_modified_time": "${formatter.format(or.lastModifiedTime)}"
           |}
           |
        """.stripMargin

      parse(json).extract[OfferResponse] mustEqual or
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

  def amountString(a: Amount): String = "%.7f".formatLocal(Locale.ROOT, a.units / math.pow(10, 7))


}

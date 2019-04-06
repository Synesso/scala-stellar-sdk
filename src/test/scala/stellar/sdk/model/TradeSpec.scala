package stellar.sdk.model

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.op.JsonSnippets

class TradeSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats = Serialization.formats(NoTypeHints) + TradeDeserializer

  "trade" should {
    "parse from json" >> prop { trade: Trade =>

      val doc =
        s"""
           |{
           |  "_links": {
           |    "self": {"href": ""},
           |    "base": {"href": "https://horizon.stellar.org/accounts/GCI7ILB37OFVHLLSA74UCXZFCTPEBJOZK7YCNBI7DKH7D76U4CRJBL2A"},
           |    "counter": {"href": "https://horizon.stellar.org/accounts/GDRFRGR2FDUFF2RI6PQE5KFSCJHGSEIOGET22R66XSATP3BYHZ46BPLO"},
           |    "operation": {"href": "https://horizon.stellar.org/operations/38583306127675393"}
           |  },
           |  "id": "${trade.id}",
           |  "paging_token": "38583306127675393-2",
           |  "ledger_close_time": "${formatter.format(trade.ledgerCloseTime)}",
           |  "offer_id": "${trade.offerId}",
           |  "base_offer_id": "${trade.baseOfferId}",
           |  "base_account": "${trade.baseAccount.accountId}",
           |  ${amountDocPortion(trade.baseAmount, "base_amount", "base_")}
           |  ${amountDocPortion(trade.counterAmount, "counter_amount", "counter_")}
           |  "counter_account": "${trade.counterAccount.accountId}",
           |  "counter_offer_id": "${trade.counterOfferId}",
           |  "base_is_seller": ${trade.baseIsSeller}
           |}
         """.stripMargin

      parse(doc).extract[Trade] mustEqual trade
    }
  }

}

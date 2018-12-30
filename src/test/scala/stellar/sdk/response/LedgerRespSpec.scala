package stellar.sdk.response

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class LedgerRespSpec extends Specification with ArbitraryInput {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

  implicit val formats = Serialization.formats(NoTypeHints) + LedgerRespDeserializer

  "a ledger response document" should {
    "parse to a ledger response" >> prop { lr: LedgerResp =>

      val json =
        s"""
           |{
           |  "_links": {
           |    "self": {
           |      "href": "http://horizon-testnet.stellar.org/ledgers/11"
           |    },
           |    "transactions": {
           |      "href": "http://horizon-testnet.stellar.org/ledgers/11/transactions{?cursor,limit,order}",
           |      "templated": true
           |    },
           |    "operations": {
           |      "href": "http://horizon-testnet.stellar.org/ledgers/11/operations{?cursor,limit,order}",
           |      "templated": true
           |    },
           |    "payments": {
           |      "href": "http://horizon-testnet.stellar.org/ledgers/11/payments{?cursor,limit,order}",
           |      "templated": true
           |    },
           |    "effects": {
           |      "href": "http://horizon-testnet.stellar.org/ledgers/11/effects{?cursor,limit,order}",
           |      "templated": true
           |    }
           |  },
           |  "id": "${lr.id}",
           |  "paging_token": "47244640256",
           |  "hash": "${lr.hash}",
           |  ${lr.previousHash.map(h => s""""prev_hash": "$h",""").getOrElse("")}
           |  "sequence": ${lr.sequence},
           |  "transaction_count": ${lr.transactionCount},
           |  "operation_count": ${lr.operationCount},
           |  "closed_at": "${formatter.format(lr.closedAt)}",
           |  "total_coins": "${lr.totalCoins}",
           |  "fee_pool": "${lr.feePool}",
           |  "base_fee_in_stroops": ${lr.baseFee},
           |  "base_reserve_in_stroops": ${lr.baseReserve},
           |  "max_tx_set_size": ${lr.maxTxSetSize},
           |  "protocol_version": 4
           |}
        """.stripMargin

      parse(json).extract[LedgerResp] must beLike { case actual: LedgerResp =>
        actual.copy(closedAt = lr.closedAt) mustEqual lr
        actual.closedAt.toInstant.toEpochMilli mustEqual lr.closedAt.toInstant.toEpochMilli
      }
    }
  }

}

package stellar.sdk.resp

import java.time.ZonedDateTime

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.Amount

case class LedgerResp(id: String, hash: String, previousHash: Option[String], sequence: Long, transactionCount: Int,
                      operationCount: Int, closedAt: ZonedDateTime, totalCoins: Double, feePool: Double, baseFee: Long,
                      baseReserve: Long, maxTxSetSize: Int)

object LedgerRespDeserializer extends ResponseParser[LedgerResp]({ o: JObject =>
  implicit val formats = DefaultFormats

  LedgerResp(
    id = (o \ "id").extract[String],
    hash = (o \ "hash").extract[String],
    previousHash = (o \ "prev_hash").extractOpt[String],
    sequence = (o \ "sequence").extract[Long],
    transactionCount = (o \ "transaction_count").extract[Int],
    operationCount = (o \ "operation_count").extract[Int],
    closedAt = ZonedDateTime.parse((o \ "closed_at").extract[String]),
    totalCoins = (o \ "total_coins").extract[String].toDouble,
    feePool = (o \ "fee_pool").extract[String].toDouble,
    baseFee = (o \ "base_fee").extractOpt[Long].getOrElse((o \ "base_fee_in_stroops").extract[Long]),
    baseReserve = {
      val old: Option[Long] = (o \ "base_reserve").extractOpt[String].map(_.toDouble).map(Amount.toBaseUnits).map(_.get)
      old.getOrElse((o \ "base_reserve_in_stroops").extract[Long])
    },
    maxTxSetSize = (o \ "max_tx_set_size").extract[Int]
  )
})

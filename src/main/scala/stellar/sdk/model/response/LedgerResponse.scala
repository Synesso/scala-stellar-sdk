package stellar.sdk.model.response

import java.time.ZonedDateTime

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.model.{Amount, NativeAmount}

case class LedgerResponse(id: String, hash: String, previousHash: Option[String], sequence: Long, successTransactionCount: Int,
                          failureTransactionCount: Int, operationCount: Int, closedAt: ZonedDateTime,
                          totalCoins: NativeAmount, feePool: NativeAmount, baseFee: NativeAmount, baseReserve: NativeAmount,
                          maxTxSetSize: Int) {

  def transactionCount: Int = successTransactionCount + failureTransactionCount

}

object LedgerRespDeserializer extends ResponseParser[LedgerResponse]({ o: JObject =>
  implicit val formats = DefaultFormats

  LedgerResponse(
    id = (o \ "id").extract[String],
    hash = (o \ "hash").extract[String],
    previousHash = (o \ "prev_hash").extractOpt[String],
    sequence = (o \ "sequence").extract[Long],
    successTransactionCount = (o \ "successful_transaction_count").extract[Int],
    failureTransactionCount = (o \ "failed_transaction_count").extract[Int],
    operationCount = (o \ "operation_count").extract[Int],
    closedAt = ZonedDateTime.parse((o \ "closed_at").extract[String]),
    totalCoins = Amount.toBaseUnits((o \ "total_coins").extract[String]).map(NativeAmount.apply).get,
    feePool = Amount.toBaseUnits((o \ "fee_pool").extract[String]).map(NativeAmount.apply).get,
    baseFee = NativeAmount((o \ "base_fee").extractOpt[Long].getOrElse((o \ "base_fee_in_stroops").extract[Long])),
    baseReserve = {
      val old: Option[Long] = (o \ "base_reserve").extractOpt[String].map(_.toDouble).map(Amount.toBaseUnits).map(_.get)
      NativeAmount(old.getOrElse((o \ "base_reserve_in_stroops").extract[Long]))
    },
    maxTxSetSize = (o \ "max_tx_set_size").extract[Int]
  )
})

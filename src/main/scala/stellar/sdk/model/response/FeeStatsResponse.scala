package stellar.sdk.model.response

import org.json4s.{DefaultFormats, JObject}
import stellar.sdk.model.NativeAmount

case class FeeStatsResponse(lastLedger: Long,
                            lastLedgerBaseFee: NativeAmount,
                            ledgerCapacityUsage: Double,
                            minAcceptedFee: NativeAmount,
                            modeAcceptedFee: NativeAmount,
                            acceptedFeePercentiles: Map[Int, NativeAmount]) {


}

object FeeStatsRespDeserializer extends ResponseParser[FeeStatsResponse]({ o: JObject =>
  implicit val formats = DefaultFormats

  def amount(field: String): NativeAmount = NativeAmount((o \ field).extract[String].toLong)

  FeeStatsResponse(
    lastLedger = (o \ "last_ledger").extract[String].toLong,
    lastLedgerBaseFee = amount("last_ledger_base_fee"),
    ledgerCapacityUsage = (o \ "ledger_capacity_usage").extract[String].toDouble,
    minAcceptedFee = amount("min_accepted_fee"),
    modeAcceptedFee = amount("mode_accepted_fee"),
    acceptedFeePercentiles = Map(
      10 -> amount("p10_accepted_fee"),
      20 -> amount("p20_accepted_fee"),
      30 -> amount("p30_accepted_fee"),
      40 -> amount("p40_accepted_fee"),
      50 -> amount("p50_accepted_fee"),
      60 -> amount("p60_accepted_fee"),
      70 -> amount("p70_accepted_fee"),
      80 -> amount("p80_accepted_fee"),
      90 -> amount("p90_accepted_fee"),
      95 -> amount("p95_accepted_fee"),
      99 -> amount("p99_accepted_fee")
    )
  )

})

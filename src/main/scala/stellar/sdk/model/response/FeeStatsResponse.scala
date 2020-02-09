package stellar.sdk.model.response

import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, JObject}
import stellar.sdk.model.NativeAmount

case class FeeStatsResponse(lastLedger: Long,
                            lastLedgerBaseFee: NativeAmount,
                            ledgerCapacityUsage: Double,
                            maxFees: FeeStats,
                            chargedFees: FeeStats) {

  @deprecated("Use `chargedFees.min` instead.", "v0.11.0")
  def minAcceptedFee: NativeAmount = chargedFees.min

  @deprecated("Use `chargedFees.mode` instead.", "v0.11.0")
  def modeAcceptedFee: NativeAmount = chargedFees.mode

  @deprecated("Use `chargedFees.percentiles` instead.", "v0.11.0")
  def acceptedFeePercentiles: Map[Int, NativeAmount] = chargedFees.percentiles

}

case class FeeStats(min: NativeAmount,
                    mode: NativeAmount,
                    max: NativeAmount,
                    percentiles: Map[Int, NativeAmount])

object FeeStatsRespDeserializer extends ResponseParser[FeeStatsResponse]({ o: JObject =>
  implicit val formats = DefaultFormats + FeeStatsDeserializer

  def amount(field: String): NativeAmount = NativeAmount((o \ field).extract[String].toLong)

  val lastLedger = (o \ "last_ledger").extract[String].toLong
  val lastLedgerBaseFee = amount("last_ledger_base_fee")
  val ledgerCapacityUsage = (o \ "ledger_capacity_usage").extract[String].toDouble
  val maxFees = (o \ "max_fee").extract[FeeStats]
  val chargedFees = (o \ "fee_charged").extract[FeeStats]

  FeeStatsResponse(lastLedger, lastLedgerBaseFee, ledgerCapacityUsage, maxFees, chargedFees)
})

object FeeStatsDeserializer extends ResponseParser[FeeStats]({ o: JObject =>
  implicit val formats = DefaultFormats

  def amount(field: String): NativeAmount = NativeAmount((o \ field).extract[String].toLong)

  FeeStats(
    min = amount("min"),
    mode = amount("mode"),
    max = amount("max"),
    percentiles = Map(
      10 -> amount("p10"),
      20 -> amount("p20"),
      30 -> amount("p30"),
      40 -> amount("p40"),
      50 -> amount("p50"),
      60 -> amount("p60"),
      70 -> amount("p70"),
      80 -> amount("p80"),
      90 -> amount("p90"),
      95 -> amount("p95"),
      99 -> amount("p99")
    ))
})

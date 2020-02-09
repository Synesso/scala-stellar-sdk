package stellar.sdk.model.response

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class FeeStatsResponseSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + FeeStatsRespDeserializer

  "a fee stats response document" should {
    "parse to a fee stats response" >> prop { r: FeeStatsResponse =>
      val json =
        s"""
           |{
           |  "last_ledger": "${r.lastLedger}",
           |  "last_ledger_base_fee": "${r.lastLedgerBaseFee.units}",
           |  "ledger_capacity_usage": "${r.ledgerCapacityUsage}",
           |  "fee_charged": {
           |    "max": "${r.chargedFees.max.units}",
           |    "min": "${r.chargedFees.min.units}",
           |    "mode": "${r.chargedFees.mode.units}",
           |    "p10": "${r.chargedFees.percentiles(10).units}",
           |    "p20": "${r.chargedFees.percentiles(20).units}",
           |    "p30": "${r.chargedFees.percentiles(30).units}",
           |    "p40": "${r.chargedFees.percentiles(40).units}",
           |    "p50": "${r.chargedFees.percentiles(50).units}",
           |    "p60": "${r.chargedFees.percentiles(60).units}",
           |    "p70": "${r.chargedFees.percentiles(70).units}",
           |    "p80": "${r.chargedFees.percentiles(80).units}",
           |    "p90": "${r.chargedFees.percentiles(90).units}",
           |    "p95": "${r.chargedFees.percentiles(95).units}",
           |    "p99": "${r.chargedFees.percentiles(99).units}"
           |  },
           |  "max_fee": {
           |    "max": "${r.maxFees.max.units}",
           |    "min": "${r.maxFees.min.units}",
           |    "mode": "${r.maxFees.mode.units}",
           |    "p10": "${r.maxFees.percentiles(10).units}",
           |    "p20": "${r.maxFees.percentiles(20).units}",
           |    "p30": "${r.maxFees.percentiles(30).units}",
           |    "p40": "${r.maxFees.percentiles(40).units}",
           |    "p50": "${r.maxFees.percentiles(50).units}",
           |    "p60": "${r.maxFees.percentiles(60).units}",
           |    "p70": "${r.maxFees.percentiles(70).units}",
           |    "p80": "${r.maxFees.percentiles(80).units}",
           |    "p90": "${r.maxFees.percentiles(90).units}",
           |    "p95": "${r.maxFees.percentiles(95).units}",
           |    "p99": "${r.maxFees.percentiles(99).units}"
           |  }
           |}
         """.stripMargin

      val actual = parse(json).extract[FeeStatsResponse]
      actual mustEqual r
      actual.acceptedFeePercentiles mustEqual actual.chargedFees.percentiles
      actual.minAcceptedFee mustEqual actual.chargedFees.min
      actual.modeAcceptedFee mustEqual actual.chargedFees.mode
    }
  }

}

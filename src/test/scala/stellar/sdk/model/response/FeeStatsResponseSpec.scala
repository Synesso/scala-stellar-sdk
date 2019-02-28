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
           |  "min_accepted_fee": "${r.minAcceptedFee.units}",
           |  "mode_accepted_fee": "${r.modeAcceptedFee.units}",
           |  "p10_accepted_fee": "${r.acceptedFeePercentiles(10).units}",
           |  "p20_accepted_fee": "${r.acceptedFeePercentiles(20).units}",
           |  "p30_accepted_fee": "${r.acceptedFeePercentiles(30).units}",
           |  "p40_accepted_fee": "${r.acceptedFeePercentiles(40).units}",
           |  "p50_accepted_fee": "${r.acceptedFeePercentiles(50).units}",
           |  "p60_accepted_fee": "${r.acceptedFeePercentiles(60).units}",
           |  "p70_accepted_fee": "${r.acceptedFeePercentiles(70).units}",
           |  "p80_accepted_fee": "${r.acceptedFeePercentiles(80).units}",
           |  "p90_accepted_fee": "${r.acceptedFeePercentiles(90).units}",
           |  "p95_accepted_fee": "${r.acceptedFeePercentiles(95).units}",
           |  "p99_accepted_fee": "${r.acceptedFeePercentiles(99).units}"
           |}
         """.stripMargin

      parse(json).extract[FeeStatsResponse] mustEqual r
    }
  }

}

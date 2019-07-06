package stellar.sdk.model

import org.json4s.NoTypeHints
import org.json4s.native.{JsonMethods, Serialization}
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class TradeAggregationSpec extends Specification with ArbitraryInput {
  implicit val formats = Serialization.formats(NoTypeHints) + TradeAggregationDeserializer

  "a payment path response document" should {
    "parse to a payment path" >> prop { ta: TradeAggregation =>
      val json =
        s"""
          |{
          |  "timestamp": ${ta.instant.toEpochMilli},
          |  "trade_count": ${ta.tradeCount},
          |  "base_volume": "${ta.baseVolume}",
          |  "counter_volume": "${ta.counterVolume}",
          |  "avg": "${ta.average}",
          |  "high": "${ta.high.asDecimalString}",
          |  "high_r": {
          |    "N": ${ta.high.n},
          |    "D": ${ta.high.d}
          |  },
          |  "low": "${ta.low.asDecimalString}",
          |  "low_r": {
          |    "N": ${ta.low.n},
          |    "D": ${ta.low.d}
          |  },
          |  "open": "${ta.open.asDecimalString}",
          |  "open_r": {
          |    "N": ${ta.open.n},
          |    "D": ${ta.open.d}
          |  },
          |  "close": "${ta.close.asDecimalString}",
          |  "close_r": {
          |    "N": ${ta.close.n},
          |    "D": ${ta.close.d}
          |  }
          |}
        """.stripMargin

      JsonMethods.parse(json).extract[TradeAggregation] mustEqual ta
    }
  }
}

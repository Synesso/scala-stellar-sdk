package stellar.sdk.model

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class AmountSpec extends Specification with ArbitraryInput {

  "an amount" should {
    "present human value as base unit * 10^-7" >> prop { amount: Amount =>
      amount.toHumanValue mustEqual amount.units / math.pow(10, 7)
    }

    "convert base unit to display unit" >> prop { l: Long =>
      Amount.toDisplayUnits(l).toDouble mustEqual (l / math.pow(10, 7))
    }.setGen(Gen.posNum[Long])

    "serde via xdr bytes" >> prop { expected: Amount =>
      val (remaining, actual) = Amount.decode.run(expected.encode).value
      actual mustEqual expected
      remaining must beEmpty
    }
  }

  "a number of units and a non-native asset" should {
    "should compose to an IssuedAmount" >> prop { (units: Long, nonNativeAsset: NonNativeAsset) =>
      Amount(units, nonNativeAsset) mustEqual IssuedAmount(units, nonNativeAsset)
    }.setGen1(Gen.posNum[Long])
  }

  "converting a number to base units" should {
    "round correctly" >> prop { l: Double =>
      val moreThan7DecimalPlaces = (l.toString.length - l.toString.indexOf('.')) > 8
      if (moreThan7DecimalPlaces) {
        Amount.toBaseUnits(l) must beAFailedTry[Long]
      } else {
        val expected = l.toString.takeWhile(_ != '.') + (l.toString + "0000000").dropWhile(_ != '.').slice(1, 8)
        Amount.toBaseUnits(l) must beASuccessfulTry[Long].like { case a => a.toString mustEqual expected }
      }
    }.setGen(Gen.posNum[Double])
  }

  "throw an exception if there are too many digits in fractional portion of lumens constructor" >> {
    Amount.lumens(0.1234567) must not(throwAn[IllegalArgumentException])
    Amount.lumens(0.12345678) must throwAn[IllegalArgumentException]
  }
}

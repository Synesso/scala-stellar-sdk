package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.mutable.Specification

class AmountSpec extends Specification with ArbitraryInput {

  "an amount" should {
    "present human value as base unit * 10^-7" >> prop { amount: Amount =>
      amount.toHumanValue mustEqual amount.units / math.pow(10, 7)
    }
  }

  "a number of units and a non-native asset" should {
    "should compose to an IssuedAmount" >> prop { (units: Long, nonNativeAsset: NonNativeAsset) =>
      Amount(units, nonNativeAsset) mustEqual IssuedAmount(units, nonNativeAsset)
    }.setGen1(Gen.posNum[Long])
  }
}

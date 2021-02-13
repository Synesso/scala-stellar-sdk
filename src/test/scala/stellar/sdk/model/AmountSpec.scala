package stellar.sdk.model

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class AmountSpec extends Specification with ArbitraryInput {

  "an amount" should {
    "convert base unit to display unit" >> prop { l: Long =>
      val displayed = NativeAmount(l).toDisplayUnits
      if (l <= 9999999L) displayed mustEqual f"0.$l%07d"
      else {
        val lStr = l.toString
        displayed mustEqual s"${lStr.take(lStr.length - 7)}.${lStr.drop(lStr.length - 7)}"
      }
    }.setGen(Gen.posNum[Long])

    "convert to base units without losing precision" >> {
      Amount.toBaseUnits("100076310227.4749892") must beASuccessfulTry(1000763102274749892L)
      Amount.toBaseUnits("100076310227.4749892").map(NativeAmount).map(_.toDisplayUnits) must beASuccessfulTry(
        "100076310227.4749892"
      )
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

    "parse from string correctly" >> {
      Amount.toBaseUnits("100076310227.4749892") must beASuccessfulTry[Long](1000763102274749892L)
      Amount.toBaseUnits("100076310227.4749") must beASuccessfulTry[Long](1000763102274749000L)
    }
  }

  "throw an exception if there are too many digits in fractional portion of lumens constructor" >> {
    Amount.lumens(0.1234567) must not(throwAn[IllegalArgumentException])
    Amount.lumens(0.12345678) must throwAn[IllegalArgumentException]
  }
}

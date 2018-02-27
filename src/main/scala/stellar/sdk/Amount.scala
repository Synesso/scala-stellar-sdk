package stellar.sdk

import java.math.{MathContext, RoundingMode}

import scala.util.Try

trait Amount {
  val units: Long
  val asset: Asset
  def toHumanValue: Double = units / math.pow(10, Amount.decimalPlaces)
}

case class NativeAmount(units: Long) extends Amount {
  override val asset: Asset = NativeAsset
}

case class IssuedAmount(units: Long, asset: NonNativeAsset) extends Amount

object Amount {
  private val decimalPlaces = 7

  def toBaseUnits(d: Double): Try[Long] = Try {
    (BigDecimal(d) * BigDecimal(math.pow(10, decimalPlaces)).round(new MathContext(0, RoundingMode.DOWN))).toLongExact
  }

  def toDisplayUnits(l: Long): String = f"${l / math.pow(10, decimalPlaces)}%.7f"

  def apply(units: Long, asset: Asset): Amount = {
    asset match {
      case NativeAsset => NativeAmount(units)
      case a: NonNativeAsset => IssuedAmount(units, a)
    }
  }

  def lumens(units: Double): Try[NativeAmount] = toBaseUnits(units).map(NativeAmount)
}

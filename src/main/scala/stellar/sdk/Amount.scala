package stellar.sdk

import java.math.{MathContext, RoundingMode}
import java.util.Locale

import cats.data.State
import stellar.sdk.xdr.{Decode, Encodable, Encode}

import scala.util.Try

trait Amount extends Encodable {
  val units: Long
  val asset: Asset

  def toHumanValue: Double = units / math.pow(10, Amount.decimalPlaces)

  def encode: Stream[Byte] = asset.encode ++ Encode.long(units)
}

case class NativeAmount(units: Long) extends Amount {
  override val asset: Asset = NativeAsset
  override def toString: String = s"$toHumanValue XLM"
}

case class IssuedAmount(units: Long, asset: NonNativeAsset) extends Amount

object Amount {
  private val decimalPlaces = 7

  def toBaseUnits(d: Double): Try[Long] = toBaseUnits(BigDecimal(d))

  def toBaseUnits(bd: BigDecimal): Try[Long] = Try {
    (bd * BigDecimal(math.pow(10, decimalPlaces)).round(new MathContext(0, RoundingMode.DOWN))).toLongExact
  }

  def toDisplayUnits(l: Long): String = "%.7f".formatLocal(Locale.ROOT, l / math.pow(10, decimalPlaces))

  def apply(units: Long, asset: Asset): Amount = {
    asset match {
      case NativeAsset => NativeAmount(units)
      case a: NonNativeAsset => IssuedAmount(units, a)
    }
  }

  /**
    * Convenience method to create native amount denoted in lumens.
    *
    * @param units quantity of lumens
    * @return NativeAmount of the given quantity
    */
  def lumens(units: Double): NativeAmount = toBaseUnits(units).map(NativeAmount).getOrElse(
    throw new IllegalArgumentException(s"Too many digits in fractional portion of $units. Limit is $decimalPlaces")
  )

  def decode: State[Seq[Byte], Amount] = for {
    asset <- Asset.decode
    units <- Decode.long
  } yield apply(units, asset)
}

object IssuedAmount {
  def decode: State[Seq[Byte], IssuedAmount] = Amount.decode.map(x => x.asInstanceOf[IssuedAmount])
}
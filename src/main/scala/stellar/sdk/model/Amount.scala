package stellar.sdk.model

import java.math.{MathContext, RoundingMode}
import java.util.Locale

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}

import scala.util.Try

sealed trait Amount extends Encodable {
  val units: Long
  val asset: Asset

  def toDisplayUnits: String = "%.7f".formatLocal(Locale.ROOT, BigDecimal(units) / Amount.toIntegralFactor)

  def encode: Stream[Byte] = asset.encode ++ Encode.long(units)
}

case class NativeAmount(units: Long) extends Amount {
  override val asset: Asset = NativeAsset
  override def toString: String = s"$toDisplayUnits XLM"
}

case class IssuedAmount(units: Long, asset: NonNativeAsset) extends Amount {
  override def toString: String = s"$toDisplayUnits $asset"
}

object Amount {
  private val decimalPlaces = 7
  private val toIntegralFactor = BigDecimal(math.pow(10, decimalPlaces))

  def toBaseUnits(d: Double): Try[Long] = toBaseUnits(BigDecimal(d))

  def toBaseUnits(s: String): Try[Long] = Try(BigDecimal(s)).flatMap(toBaseUnits)

  def toBaseUnits(bd: BigDecimal): Try[Long] = Try {
    (bd * toIntegralFactor.round(new MathContext(0, RoundingMode.DOWN))).toLongExact
  }

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
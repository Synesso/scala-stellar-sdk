package stellar.sdk.model

import java.math.{MathContext, RoundingMode}
import java.util.Locale

import org.json4s.{DefaultFormats, Formats, JObject}

import scala.util.Try

sealed trait Amount {
  val units: Long
  val asset: Asset

  def toDisplayUnits: String = "%.7f".formatLocal(Locale.ROOT, BigDecimal(units) / Amount.toIntegralFactor)
}

case class NativeAmount(units: Long) extends Amount {
  override val asset: Asset = NativeAsset
  override def toString: String = s"$toDisplayUnits XLM"
}

case class IssuedAmount(units: Long, asset: NonNativeAsset) extends Amount {
  override def toString: String = s"$toDisplayUnits $asset"
}

object Amount {
  implicit val formats: Formats = DefaultFormats
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

  def doubleFromString(o: JObject, key: String): Double = (o \ key).extract[String].toDouble

  def parseNativeAmount(o: JObject, key: String): NativeAmount = {
    NativeAmount(Amount.toBaseUnits(doubleFromString(o, key)).get)
  }

  def parseIssuedAmount(o: JObject, label: String): IssuedAmount = parseAmount(o, label).asInstanceOf[IssuedAmount]

  def parseAmount(o: JObject, label: String = "amount", assetPrefix: String = ""): Amount = {
    val units = Amount.toBaseUnits(doubleFromString(o, label)).get
    Asset.parseAsset(assetPrefix, o) match {
      case nna: NonNativeAsset => IssuedAmount(units, nna)
      case NativeAsset => NativeAmount(units)
    }
  }
}

object IssuedAmount {
}
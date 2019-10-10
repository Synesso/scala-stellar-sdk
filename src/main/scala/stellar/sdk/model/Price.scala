package stellar.sdk.model

import java.util.Locale

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encode}

case class Price(n: Int, d: Int) {
  def asDecimalString: String = "%.7f".formatLocal(Locale.ROOT, n * 1.0 / d * 1.0)

  def encode: Stream[Byte] = Encode.int(n) ++ Encode.int(d)

  def asBigDecimal: BigDecimal = BigDecimal(n) / BigDecimal(d)

  def >(other: Price): Boolean = asBigDecimal > other.asBigDecimal
  def >=(other: Price): Boolean = asBigDecimal >= other.asBigDecimal
  def <(other: Price): Boolean = asBigDecimal < other.asBigDecimal
  def <=(other: Price): Boolean = asBigDecimal <= other.asBigDecimal

  override def toString: String = s"$asDecimalString ($n/$d)"
}

object Price extends Decode {
  def decode: State[Seq[Byte], Price] = for {
    n <- int
    d <- int
  } yield Price(n, d)
}

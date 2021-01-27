package stellar.sdk.model

import java.util.Locale

import cats.data.State
import org.stellar.xdr.{Int32, Price => XPrice}
import stellar.sdk.model.xdr.{Decode, Encode}

case class Price(n: Int, d: Int) {
  def xdr: XPrice = new XPrice.Builder()
    .d(new Int32(d))
    .n(new Int32(n))
    .build()

  def asDecimalString = "%.7f".formatLocal(Locale.ROOT, n * 1.0 / d * 1.0)

  def encode: LazyList[Byte] = Encode.int(n) ++ Encode.int(d)

  // TODO (jem): As BigDecimal

  override def toString: String = s"$n:$d"
}

object Price extends Decode {
  def decodeXdr(xdr: XPrice): Price = Price(xdr.getN.getInt32, xdr.getD.getInt32)
  def decode: State[Seq[Byte], Price] = for {
    n <- int
    d <- int
  } yield Price(n, d)
}

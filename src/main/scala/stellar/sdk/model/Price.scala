package stellar.sdk.model

import java.util.Locale

import org.stellar.xdr.{Price => XPrice}
import org.stellar.xdr.Int32

case class Price(n: Int, d: Int) {

  def asDecimalString: String = "%.7f".formatLocal(Locale.ROOT, n * 1.0 / d * 1.0)

  def xdr: XPrice = new XPrice.Builder()
    .d(new Int32(d))
    .n(new Int32(n))
    .build()

  override def toString: String = s"$n:$d"
}

object Price {
  def decode(xdr: XPrice): Price = Price(xdr.getN.getInt32, xdr.getD.getInt32)
}

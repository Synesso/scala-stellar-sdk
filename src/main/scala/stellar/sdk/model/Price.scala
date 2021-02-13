package stellar.sdk.model

import java.util.Locale

import org.stellar.xdr.{Int32, Price => XPrice}

case class Price(n: Int, d: Int) {
  def xdr: XPrice = new XPrice.Builder()
    .d(new Int32(d))
    .n(new Int32(n))
    .build()

  def asDecimalString = "%.7f".formatLocal(Locale.ROOT, n * 1.0 / d * 1.0)

  // TODO (jem): As BigDecimal

  override def toString: String = s"$n:$d"
}

object Price {
  def decodeXdr(xdr: XPrice): Price = Price(xdr.getN.getInt32, xdr.getD.getInt32)
}

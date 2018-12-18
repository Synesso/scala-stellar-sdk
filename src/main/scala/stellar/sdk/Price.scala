package stellar.sdk

import java.util.Locale

import org.stellar.sdk.xdr.{Price => XDRPrice}
import stellar.sdk.XDRPrimitives._

case class Price(n: Int, d: Int) extends Encodable {
  def toXDR = {
    val xdr = new XDRPrice
    xdr.setN(int32(n))
    xdr.setD(int32(d))
    xdr
  }

  def asDecimalString = "%.7f".formatLocal(Locale.ROOT, n * 1.0 / d * 1.0)

  override def encode: Stream[Byte] = Encode.int(n) ++ Encode.int(d)
}

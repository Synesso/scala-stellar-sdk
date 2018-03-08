package stellar.sdk

import org.stellar.sdk.xdr.{Price => XDRPrice}
import stellar.sdk.XDRPrimitives._

case class Price(n: Int, d: Int) {
  def toXDR = {
    val xdr = new XDRPrice
    xdr.setN(int32(n))
    xdr.setD(int32(d))
    xdr
  }

  def asDecimalString = f"${n * 1.0 / d * 1.0}%.7f"

}

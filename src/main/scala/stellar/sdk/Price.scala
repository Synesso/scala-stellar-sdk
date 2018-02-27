package stellar.sdk

import org.stellar.sdk.xdr.{Price => XDRPrice}

case class Price(n: Int, d: Int) extends XDRPrimitives {
  def toXDR = {
    val xdr = new XDRPrice
    xdr.setN(int32(n))
    xdr.setD(int32(d))
    xdr
  }

  def asDecimalString = f"${n * 1.0 / d * 1.0}%.7f"

}

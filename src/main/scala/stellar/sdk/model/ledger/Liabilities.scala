package stellar.sdk.model.ledger

import org.stellar.xdr.{Int64, Liabilities => XLiabilities}

case class Liabilities(buying: Long, selling: Long) {
  def xdr: XLiabilities = new XLiabilities.Builder()
    .buying(new Int64(buying))
    .selling(new Int64(selling))
    .build()
}

object Liabilities {

  def decodeXdr(xdr: XLiabilities): Liabilities = Liabilities(
    buying = xdr.getBuying.getInt64,
    selling = xdr.getSelling.getInt64
  )
}

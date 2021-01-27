package stellar.sdk.model.ledger

import cats.data.State
import org.stellar.xdr.{Int64, Liabilities => XLiabilities}
import stellar.sdk.model.xdr.Encode._
import stellar.sdk.model.xdr.{Decode, Encodable}

case class Liabilities(buying: Long, selling: Long) extends Encodable {
  def xdr: XLiabilities = new XLiabilities.Builder()
    .buying(new Int64(buying))
    .selling(new Int64(selling))
    .build()

  override def encode: LazyList[Byte] = long(buying) ++ long(selling) ++ int(0)
}

object Liabilities extends Decode {

  def decodeXdr(xdr: XLiabilities): Liabilities = Liabilities(
    buying = xdr.getBuying.getInt64,
    selling = xdr.getSelling.getInt64
  )

  val decode: State[Seq[Byte], Liabilities] = for {
    buying <- long
    selling <- long
    _ <- int
  } yield Liabilities(buying, selling)
}

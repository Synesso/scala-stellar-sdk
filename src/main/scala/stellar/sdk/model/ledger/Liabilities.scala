package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable}
import stellar.sdk.model.xdr.Encode._

case class Liabilities(buying: Long, selling: Long) extends Encodable {
  override def encode: LazyList[Byte] = long(buying) ++ long(selling) ++ int(0)
}

object Liabilities extends Decode {
  val decode: State[Seq[Byte], Liabilities] = for {
    buying <- long
    selling <- long
    _ <- int
  } yield Liabilities(buying, selling)
}

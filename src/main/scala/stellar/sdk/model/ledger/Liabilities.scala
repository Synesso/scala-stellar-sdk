package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.xdr.Decode

case class Liabilities(buying: Long, selling: Long)

object Liabilities extends Decode {
  val decode: State[Seq[Byte], Liabilities] = for {
    buying <- long
    selling <- long
    _ <- int
  } yield Liabilities(buying, selling)
}

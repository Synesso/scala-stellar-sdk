package stellar.sdk.model.ledger

import cats.data.{NonEmptyList, State}
import stellar.sdk.model.xdr.Decode

sealed trait LedgerEntryChange
case class LedgerEntryState(entry: LedgerEntry) extends LedgerEntryChange
case class LedgerEntryCreate(entry: LedgerEntry) extends LedgerEntryChange
case class LedgerEntryUpdate(entry: LedgerEntry) extends LedgerEntryChange
case class LedgerEntryDelete(entry: LedgerKey) extends LedgerEntryChange

object LedgerEntryState extends Decode {
  def decode: State[Seq[Byte], LedgerEntryState] = for {
    discriminator <- int
    entry <- LedgerEntry.decode
    _ <- int
  } yield discriminator match {
    case 3 => LedgerEntryState(entry)
    case _ => throw new IllegalArgumentException(
      s"Attempted to load LedgerEntryState, but discriminator was $discriminator")
  }
}

object LedgerEntryChange extends Decode {

  val decode: State[Seq[Byte], LedgerEntryChange] = for {
    entry <- switch[LedgerEntryChange](
      widen(LedgerEntry.decode.map(LedgerEntryCreate).flatMap(drop(int))),
      widen(LedgerEntry.decode.map(LedgerEntryUpdate).flatMap(drop(int))),
      widen(LedgerKey.decode.map(LedgerEntryDelete)),
      widen(LedgerEntry.decode.map(LedgerEntryCreate).flatMap(drop(int)))
    )
  } yield entry
}


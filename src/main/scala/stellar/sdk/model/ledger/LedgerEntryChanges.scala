package stellar.sdk.model.ledger

import cats.data.{NonEmptyList, State}
import stellar.sdk.model.xdr.Decode

case class LedgerEntryChanges(initial: LedgerEntryState, changes: NonEmptyList[LedgerEntryChange])

object LedgerEntryChanges extends Decode {
  def decode: State[Seq[Byte], Option[LedgerEntryChanges]] = int.flatMap {
    case 0 => State.pure(Option.empty[LedgerEntryChanges])
    case n => for {
      init <- LedgerEntryState.decode
      tail <- seq(n - 1, LedgerEntryChange.decode)
    } yield Option(LedgerEntryChanges(init, NonEmptyList.of(tail.head, tail.tail: _*)))
  }
}

sealed trait LedgerEntryOp
sealed trait LedgerEntryChange extends LedgerEntryOp
case class LedgerEntryState(entry: LedgerEntry) extends LedgerEntryOp
case class LedgerEntryCreate(entry: LedgerEntry) extends LedgerEntryChange
case class LedgerEntryUpdate(entry: LedgerEntry) extends LedgerEntryChange
// TODO (jem) - I've never seen a delete. Find one.
case class LedgerEntryDelete(entry: LedgerEntry) extends LedgerEntryChange

object LedgerEntryState extends Decode {
  def decode: State[Seq[Byte], LedgerEntryState] = for {
    discriminator <- int
    entry <- LedgerEntry.decode
  } yield discriminator match {
    case 3 => LedgerEntryState(entry)
    case _ => throw new IllegalArgumentException(
      s"Attempted to load LedgerEntryState, but discriminator was $discriminator")
  }
}

object LedgerEntryChange extends Decode {
  def decode: State[Seq[Byte], LedgerEntryChange] = for {
    discriminator <- int
    _ = require(discriminator != 3, s"Attempted to load LedgerEntryChange, but discriminator was 3")
    entry <- LedgerEntry.decode
  } yield discriminator match {
    case 0 => LedgerEntryCreate(entry)
    case 1 => LedgerEntryUpdate(entry)
    case 2 => LedgerEntryDelete(entry)
    case _ => throw new IllegalArgumentException(
      s"Attempted to load LedgerEntryState, but discriminator was $discriminator")
  }
}


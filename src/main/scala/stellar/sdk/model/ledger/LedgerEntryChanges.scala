package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.ledger.LedgerEntry.{int, switch, widen}
import stellar.sdk.model.ledger.TransactionLedgerEntries.{arr, decode}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays

sealed trait LedgerEntryChange extends Encodable

case class LedgerEntryCreate(entry: LedgerEntry) extends LedgerEntryChange {
  override def encode: Stream[Byte] = Encode.int(0) ++ entry.encode ++ Encode.int(0)
}

case class LedgerEntryUpdate(entry: LedgerEntry) extends LedgerEntryChange {
  override def encode: Stream[Byte] = Encode.int(1) ++ entry.encode ++ Encode.int(0)
}

case class LedgerEntryDelete(entry: LedgerKey) extends LedgerEntryChange {
  override def encode: Stream[Byte] = Encode.int(2) ++ entry.encode
}

case class LedgerEntryState(entry: LedgerEntry) extends LedgerEntryChange {
  override def encode: Stream[Byte] = Encode.int(3) ++ entry.encode ++ Encode.int(0)
}

object LedgerEntryChange extends Decode {

  val decode: State[Seq[Byte], LedgerEntryChange] = switch[LedgerEntryChange](
    widen(LedgerEntry.decode.map(LedgerEntryCreate).flatMap(drop(int))),
    widen(LedgerEntry.decode.map(LedgerEntryUpdate).flatMap(drop(int))),
    widen(LedgerKey.decode.map(LedgerEntryDelete)),
    widen(LedgerEntry.decode.map(LedgerEntryState).flatMap(drop(int)))
  )
}

object LedgerEntryChanges {

  def decodeXDR(base64: String): Seq[LedgerEntryChange] =
    arr(LedgerEntryChange.decode).run(ByteArrays.base64(base64)).value._2

}

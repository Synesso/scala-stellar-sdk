package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable, Encode, Encoded}
import stellar.sdk.util.ByteArrays

case class TransactionLedgerEntries(txnLevelChanges: Option[Seq[LedgerEntryChange]],
                                    operationLevelChanges: Seq[Seq[LedgerEntryChange]]) extends Encodable {

  override def encode: Stream[Byte] = txnLevelChanges.map(encode1).getOrElse(encode0)

  private def encode0: Stream[Byte] = Encode.int(0) ++
    Encode.arr(operationLevelChanges.map(Encode.arr(_)).map(Encoded))

  private def encode1(txnLevel: Seq[LedgerEntryChange]): Stream[Byte] = Encode.int(1) ++
    Encode.arr(txnLevel) ++
    Encode.arr(operationLevelChanges.map(Encode.arr(_)).map(Encoded))
}

object TransactionLedgerEntries extends Decode {

  def decodeXDR(base64: String) = decode.run(ByteArrays.base64(base64)).value._2

  private val decodeV0: State[Seq[Byte], TransactionLedgerEntries] = for {
    ops <- arr(arr(LedgerEntryChange.decode))
  } yield TransactionLedgerEntries(None, ops)

  private val decodeV1: State[Seq[Byte], TransactionLedgerEntries] = for {
    txnLevelChanges <- arr(LedgerEntryChange.decode)
    ops <- arr(arr(LedgerEntryChange.decode))
  } yield TransactionLedgerEntries(Some(txnLevelChanges), ops)

  val decode: State[Seq[Byte], TransactionLedgerEntries] = switch(decodeV0, decodeV1)

}



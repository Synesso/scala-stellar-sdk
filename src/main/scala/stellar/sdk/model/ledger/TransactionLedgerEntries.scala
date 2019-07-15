package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.xdr.Decode
import stellar.sdk.util.ByteArrays

case class TransactionLedgerEntries(txnLevelChanges: Seq[LedgerEntryChange], operationLevelChanges: Seq[Seq[LedgerEntryChange]])

object TransactionLedgerEntries extends Decode {

  def decodeXDR(base64: String) = decode.run(ByteArrays.base64(base64)).value._2

  private val decodeV0: State[Seq[Byte], TransactionLedgerEntries] = for {
    ops <- arr(arr(LedgerEntryChange.decode))
  } yield TransactionLedgerEntries(Nil, ops)

  private val decodeV1: State[Seq[Byte], TransactionLedgerEntries] = for {
    txnLevelChanges <- arr(LedgerEntryChange.decode)
    ops <- arr(arr(LedgerEntryChange.decode))
  } yield TransactionLedgerEntries(txnLevelChanges, ops)

  val decode: State[Seq[Byte], TransactionLedgerEntries] = switch(decodeV0, decodeV1)

}



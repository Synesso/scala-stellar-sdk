package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.xdr.Decode
import stellar.sdk.util.ByteArrays

case class TransactionLedgerEntries(txnLevelChanges: Option[LedgerEntryChanges], operationLevelChanges: Seq[LedgerEntryChanges])

object TransactionLedgerEntries extends Decode {

  def decodeXDR(base64: String) = decode.run(ByteArrays.base64(base64)).value._2

  val decode: State[Seq[Byte], TransactionLedgerEntries] = switch(decodeV0, decodeV1)

  private def decodeV0: State[Seq[Byte], TransactionLedgerEntries] = for {
    ops <- arr(LedgerEntryChanges.decode)
  } yield TransactionLedgerEntries(None, ops.flatten)

  private def decodeV1: State[Seq[Byte], TransactionLedgerEntries] = for {
    txnLevelChanges <- LedgerEntryChanges.decode
    ops <- arr(LedgerEntryChanges.decode)
  } yield TransactionLedgerEntries(txnLevelChanges, ops.flatten)

}



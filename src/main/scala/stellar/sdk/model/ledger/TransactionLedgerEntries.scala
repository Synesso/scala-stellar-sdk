package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable, Encode, Encoded}
import stellar.sdk.util.ByteArrays

/**
  * Meta data about the effect a transaction had on the ledger it was transacted in.
  * @param txnLevelChanges the ledger changes caused by the transaction itself (not any one specific operation).
  *                        In earlier versions of the protocol, this field was not present. In such cases the field will
  *                        be `None`.
  * @param operationLevelChanges the ledger changes caused by the individual operations. The order of the outer sequence
  *                              matched the order of operations in the transaction.
  */
case class TransactionLedgerEntries(txnLevelChanges: Option[Seq[LedgerEntryChange]],
                                    operationLevelChanges: Seq[Seq[LedgerEntryChange]]) extends Encodable {

  override def encode: LazyList[Byte] = txnLevelChanges.map(encode1).getOrElse(encode0)

  private def encode0: LazyList[Byte] = Encode.int(0) ++
    Encode.arr(operationLevelChanges.map(Encode.arr(_)).map(Encoded))

  private def encode1(txnLevel: Seq[LedgerEntryChange]): LazyList[Byte] = Encode.int(1) ++
    Encode.arr(txnLevel) ++
    Encode.arr(operationLevelChanges.map(Encode.arr(_)).map(Encoded))
}

object TransactionLedgerEntries extends Decode {

  def decodeXDR(base64: String): TransactionLedgerEntries = decode.run(ByteArrays.base64(base64).toIndexedSeq).value._2

  private val decodeV0: State[Seq[Byte], TransactionLedgerEntries] = for {
    ops <- arr(arr(LedgerEntryChange.decode))
  } yield TransactionLedgerEntries(None, ops)

  private val decodeV1: State[Seq[Byte], TransactionLedgerEntries] = for {
    txnLevelChanges <- arr(LedgerEntryChange.decode)
    ops <- arr(arr(LedgerEntryChange.decode))
  } yield TransactionLedgerEntries(Some(txnLevelChanges), ops)

  val decode: State[Seq[Byte], TransactionLedgerEntries] = switch(decodeV0, decodeV1)

}



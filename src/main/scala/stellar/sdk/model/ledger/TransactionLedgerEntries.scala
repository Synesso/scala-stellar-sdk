package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable, Encode, Encoded}
import stellar.sdk.util.ByteArrays

/**
  * Meta data about the effect a transaction had on the ledger it was transacted in.
  * @param txnLevelChanges the ledger changes caused by the transactions themselves (not any one specific operation).
  *                        The first value is optional and represents the changes preceding the transaction (introduced in version 2 of this datatype).
  *                        The second value represents the changes following the transaction (introduced in version 1 of this datatype).
  *                        In earlier versions of the protocol, this field was not present. In such cases the field will be `None`.
  * @param operationLevelChanges the ledger changes caused by the individual operations. The order of the outer sequence
  *                              matched the order of operations in the transaction.
  */
case class TransactionLedgerEntries(txnLevelChanges: Option[(Option[Seq[LedgerEntryChange]], Seq[LedgerEntryChange])],
                                    operationLevelChanges: Seq[Seq[LedgerEntryChange]]) extends Encodable {

  val txnLevelChangesBefore: Option[Seq[LedgerEntryChange]] = txnLevelChanges.flatMap(_._1)
  val txnLevelChangesAfter: Option[Seq[LedgerEntryChange]] = txnLevelChanges.map(_._2)

  override def encode: LazyList[Byte] = txnLevelChanges match {
    case Some((Some(before), after)) => encode2(before, after)
    case Some((_, after)) => encode1(after)
    case _ => encode0
  }

  private def encode0: LazyList[Byte] = Encode.int(0) ++
    Encode.arr(operationLevelChanges.map(Encode.arr(_)).map(Encoded))

  private def encode1(txnLevel: Seq[LedgerEntryChange]): LazyList[Byte] = Encode.int(1) ++
    Encode.arr(txnLevel) ++
    Encode.arr(operationLevelChanges.map(Encode.arr(_)).map(Encoded))

  private def encode2(before: Seq[LedgerEntryChange], after: Seq[LedgerEntryChange]): LazyList[Byte] = Encode.int(2) ++
    Encode.arr(before) ++
    Encode.arr(operationLevelChanges.map(Encode.arr(_)).map(Encoded)) ++
    Encode.arr(after)

}

object TransactionLedgerEntries extends Decode {

  def decodeXDR(base64: String): TransactionLedgerEntries = decode.run(ByteArrays.base64(base64).toIndexedSeq).value._2

  private val decodeV0: State[Seq[Byte], TransactionLedgerEntries] = for {
    ops <- arr(arr(LedgerEntryChange.decode))
  } yield TransactionLedgerEntries(None, ops)

  private val decodeV1: State[Seq[Byte], TransactionLedgerEntries] = for {
    txnLevelChanges <- arr(LedgerEntryChange.decode)
    ops <- arr(arr(LedgerEntryChange.decode))
  } yield TransactionLedgerEntries(Some(None, txnLevelChanges), ops)

  private val decodeV2: State[Seq[Byte], TransactionLedgerEntries] = for {
    txnLevelChangesBefore <- arr(LedgerEntryChange.decode)
    ops <- arr(arr(LedgerEntryChange.decode))
    txnLevelChangesAfter <- arr(LedgerEntryChange.decode)
  } yield TransactionLedgerEntries(Some(Some(txnLevelChangesBefore), txnLevelChangesAfter), ops)

  val decode: State[Seq[Byte], TransactionLedgerEntries] = switch(decodeV0, decodeV1, decodeV2)

}



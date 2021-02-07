package stellar.sdk.model.ledger

import okio.ByteString
import org.stellar.xdr.{TransactionMeta, TransactionMetaV1, TransactionMetaV2}

/**
  * Meta data about the effect a transaction had on the ledger it was transacted in.
 *
 * @param txnLevelChangesBefore the ledger changes caused by the transactions themselves (not any one
 *                              specific operation) preceding the transaction (introduced in version 2 of this datatype).
 *                        In earlier versions of the protocol, this field was not present. In such cases the field will be empty.
 * @param operationLevelChanges the ledger changes caused by the individual operations. The order of the outer sequence
 *                              matched the order of operations in the transaction.
 * @param txnLevelChangesAfter represents the changes following the transaction (introduced in version 1 of this datatype).
  *                        In earlier versions of the protocol, this field was not present. In such cases the field will be empty.
  */
case class TransactionLedgerEntries(
  txnLevelChangesBefore: List[LedgerEntryChange],
  operationLevelChanges: List[List[LedgerEntryChange]],
  txnLevelChangesAfter: List[LedgerEntryChange]
)

object TransactionLedgerEntries {

  def decodeXDR(base64: String): TransactionLedgerEntries = {
    val meta = TransactionMeta.decode(ByteString.decodeBase64(base64))
    meta.getDiscriminant.toInt match {
      case 0 => decodeXdr(meta)
      case 1 => decodeXdr(meta.getV1)
      case 2 => decodeXdr(meta.getV2)
    }
  }

  private def decodeXdr(meta: TransactionMeta): TransactionLedgerEntries = TransactionLedgerEntries(
    Nil, meta.getOperations.map(_.getChanges.getLedgerEntryChanges.map(LedgerEntryChange.decodeXdr).toList).toList, Nil
  )

  private def decodeXdr(meta: TransactionMetaV1): TransactionLedgerEntries = TransactionLedgerEntries(
    txnLevelChangesBefore = Nil,
    operationLevelChanges = meta.getOperations.map(_.getChanges.getLedgerEntryChanges.map(LedgerEntryChange.decodeXdr).toList).toList,
    txnLevelChangesAfter = meta.getTxChanges.getLedgerEntryChanges.map(LedgerEntryChange.decodeXdr).toList
  )

  private def decodeXdr(meta: TransactionMetaV2): TransactionLedgerEntries =
    TransactionLedgerEntries(
      txnLevelChangesBefore = meta.getTxChangesBefore.getLedgerEntryChanges.map(LedgerEntryChange.decodeXdr).toList,
      operationLevelChanges = meta.getOperations.map(_.getChanges.getLedgerEntryChanges.map(LedgerEntryChange.decodeXdr).toList).toList,
      txnLevelChangesAfter = meta.getTxChangesAfter.getLedgerEntryChanges.map(LedgerEntryChange.decodeXdr).toList
    )
}



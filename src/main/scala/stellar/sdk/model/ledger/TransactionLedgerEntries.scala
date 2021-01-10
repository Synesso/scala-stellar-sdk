package stellar.sdk.model.ledger

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
                                    operationLevelChanges: Seq[Seq[LedgerEntryChange]]) {

  val txnLevelChangesBefore: Option[Seq[LedgerEntryChange]] = txnLevelChanges.flatMap(_._1)
  val txnLevelChangesAfter: Option[Seq[LedgerEntryChange]] = txnLevelChanges.map(_._2)

//  override def encode: LazyList[Byte] = txnLevelChanges match {
//    case Some((Some(before), after)) => encode2(before, after)
//    case Some((_, after)) => encode1(after)
//    case _ => encode0
//  }

}

object TransactionLedgerEntries {
  def decodeXDR(resultMetaXDR: String): TransactionLedgerEntries = ???
}



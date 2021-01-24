package stellar.sdk.model.ledger

import org.stellar.xdr.{LedgerEntryChangeType, LedgerEntryChange => XLedgerEntryChange}

sealed trait LedgerEntryChange {
  def xdr: XLedgerEntryChange
}

case class LedgerEntryCreate(entry: LedgerEntry) extends LedgerEntryChange {
  override def xdr: XLedgerEntryChange = new XLedgerEntryChange.Builder()
    .discriminant(LedgerEntryChangeType.LEDGER_ENTRY_CREATED)
    .created(entry.xdr)
    .build()
}

case class LedgerEntryUpdate(entry: LedgerEntry) extends LedgerEntryChange {
  override def xdr: XLedgerEntryChange = new XLedgerEntryChange.Builder()
    .discriminant(LedgerEntryChangeType.LEDGER_ENTRY_UPDATED)
    .updated(entry.xdr)
    .build()
}

case class LedgerEntryDelete(entry: LedgerKey) extends LedgerEntryChange {
  override def xdr: XLedgerEntryChange = new XLedgerEntryChange.Builder()
    .discriminant(LedgerEntryChangeType.LEDGER_ENTRY_REMOVED)
    .removed(entry.xdr)
    .build()
}

case class LedgerEntryState(entry: LedgerEntry) extends LedgerEntryChange {
  override def xdr: XLedgerEntryChange = new XLedgerEntryChange.Builder()
    .discriminant(LedgerEntryChangeType.LEDGER_ENTRY_STATE)
    .state(entry.xdr)
    .build()
}

object LedgerEntryChange {
  def decode(xdr: XLedgerEntryChange): LedgerEntryChange = xdr.getDiscriminant match {
    case LedgerEntryChangeType.LEDGER_ENTRY_CREATED => LedgerEntryCreate(LedgerEntry.decode(xdr.getCreated))
    case LedgerEntryChangeType.LEDGER_ENTRY_UPDATED => LedgerEntryUpdate(LedgerEntry.decode(xdr.getUpdated))
    case LedgerEntryChangeType.LEDGER_ENTRY_REMOVED => LedgerEntryDelete(LedgerKey.decode(xdr.getRemoved))
    case LedgerEntryChangeType.LEDGER_ENTRY_STATE => LedgerEntryState(LedgerEntry.decode(xdr.getState))
  }
}

object LedgerEntryChanges {
  def decodeXDR(feeMetaXDR: String): Seq[LedgerEntryChange] = ???
}

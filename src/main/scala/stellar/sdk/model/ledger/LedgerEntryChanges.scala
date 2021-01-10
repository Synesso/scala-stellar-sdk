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
}

case class LedgerEntryDelete(entry: LedgerKey) extends LedgerEntryChange {
}

case class LedgerEntryState(entry: LedgerEntry) extends LedgerEntryChange {
}

object LedgerEntryChange {
}

object LedgerEntryChanges {
  def decodeXDR(feeMetaXDR: String): Seq[LedgerEntryChange] = ???
}

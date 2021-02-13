package stellar.sdk.model.ledger

import okio.ByteString
import org.stellar.xdr.{LedgerEntryChangeType, LedgerEntry => XLedgerEntry, LedgerEntryChange => XLedgerEntryChange, LedgerEntryChanges => XLedgerEntryChanges}

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

  def decodeXdr(xdr: XLedgerEntryChange): LedgerEntryChange =
    xdr.getDiscriminant match {
      case LedgerEntryChangeType.LEDGER_ENTRY_CREATED => LedgerEntryCreate(LedgerEntry.decodeXdr(xdr.getCreated))
      case LedgerEntryChangeType.LEDGER_ENTRY_UPDATED => LedgerEntryUpdate(LedgerEntry.decodeXdr(xdr.getUpdated))
      case LedgerEntryChangeType.LEDGER_ENTRY_REMOVED => LedgerEntryDelete(LedgerKey.decodeXdr(xdr.getRemoved))
      case LedgerEntryChangeType.LEDGER_ENTRY_STATE => LedgerEntryState(LedgerEntry.decodeXdr(xdr.getState))
    }
}

object LedgerEntryChanges {

  def decodeXDR(base64: String): List[LedgerEntryChange] = {
    XLedgerEntryChanges.decode(ByteString.decodeBase64(base64))
      .getLedgerEntryChanges.toList.map(LedgerEntryChange.decodeXdr)
  }

}

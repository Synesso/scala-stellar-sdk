package stellar.sdk.model.ledger

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification

class LedgerEntrySpec extends Specification with LedgerEntryGenerators with LazyLogging {

  "a ledger entry" should {
    "serde to/from XDR" >> prop { entry: LedgerEntry =>
      LedgerEntry.decodeXdr(entry.xdr) mustEqual entry
    }
  }

}

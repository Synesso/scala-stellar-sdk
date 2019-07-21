package stellar.sdk.model.ledger

import org.specs2.mutable.Specification

class LedgerEntrySpec extends Specification with LedgerEntryGenerators {

  "a ledger entry" should {
    "serde to/from XDR" >> prop { entry: LedgerEntry =>
      LedgerEntry.decode.run(entry.encode.toArray).value._2 mustEqual entry
    }
  }

}

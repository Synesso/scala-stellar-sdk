package stellar.sdk.model.ledger

import org.specs2.mutable.Specification

class LedgerKeySpec extends Specification with LedgerEntryGenerators {

  "a ledger key" should {
    "serde to/from XDR" >> prop { ledgerKey: LedgerKey =>
      LedgerKey.decode.run(ledgerKey.encode).value._2 mustEqual ledgerKey
    }
  }

}

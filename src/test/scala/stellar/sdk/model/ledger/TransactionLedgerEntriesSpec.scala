package stellar.sdk.model.ledger

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification

class TransactionLedgerEntriesSpec extends Specification with LedgerEntryGenerators with LazyLogging {

  "a ledger entry" should {
    "parse results with sponsorship data" >> {
      // https://horizon.stellar.org/transactions/680eb3798b6a0cd3ed37ac20cef8a2077392cd7cc886e67c8629b14c765e809c
      TransactionLedgerEntries.decodeXDR("AAAAAgAAAAIAAAADAgJjjwAAAAAAAAAA35UHgEst0l96gXTDldD6W84qtk5mJxaaS4" +
        "mAN7JfHA8AAAAAAxsSrAICYs0AAABLAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAABAgJjjwAAAAAAAAAA35UHgEst0l96g" +
        "XTDldD6W84qtk5mJxaaS4mAN7JfHA8AAAAAAxsSrAICYs0AAABMAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAABAAAABAAA" +
        "AAMB//22AAAAAAAAAADyHjqve6PjrxaK/3Rz9n8/NkfQOwpysl2J/d3rLr0+hwAAAANLZ9y4AelFCwAAABQAAAARAAAAAAAAAAAAAAAAAQA" +
        "AAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAJAAAAAAAAAAAAAAAAAAAAAAAAAAECAmOPAAAAAAAAAADyHjqve6PjrxaK/3Rz9n" +
        "8/NkfQOwpysl2J/d3rLr0+hwAAAANLZ/0kAelFCwAAABQAAAARAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAA" +
        "AIAAAAJAAAAAAAAAAAAAAAAAAAAAAAAAAMCAmOPAAAAAAAAAADflQeASy3SX3qBdMOV0Ppbziq2TmYnFppLiYA3sl8cDwAAAAADGxKsAgJi" +
        "zQAAAEwAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAECAmOPAAAAAAAAAADflQeASy3SX3qBdMOV0Ppbziq2TmYnFppLiYA" +
        "3sl8cDwAAAAADGvJAAgJizQAAAEwAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAA=")
      ok
    }
  }

}
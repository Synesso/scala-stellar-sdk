package stellar.sdk.model.ledger

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification

import scala.util.{Failure, Try}

class LedgerEntrySpec extends Specification with LedgerEntryGenerators with LazyLogging {

  "a ledger entry" should {
    "serde to/from XDR" >> prop { entry: LedgerEntry =>
      LedgerEntry.decode(entry.xdr) mustEqual entry
    }
  }

}

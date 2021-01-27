package stellar.sdk.model.ledger

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification

import scala.util.{Failure, Try}

class LedgerEntryChangeSpec extends Specification with LedgerEntryGenerators with LazyLogging {

  "a ledger entry change" should {
    "serde to/from XDR" >> prop { change: LedgerEntryChange =>
      LedgerEntryChange.decodeXdr(change.xdr) mustEqual change
    }
  }

}

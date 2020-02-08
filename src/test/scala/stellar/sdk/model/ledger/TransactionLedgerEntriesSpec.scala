package stellar.sdk.model.ledger

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification

import scala.util.{Failure, Try}

class TransactionLedgerEntriesSpec extends Specification with LedgerEntryGenerators with LazyLogging {

  "a ledger entry" should {
    "serde to/from XDR" >> prop { entries: TransactionLedgerEntries =>
      val triedEntries = Try(TransactionLedgerEntries.decode.run(entries.encode).value._2)
      triedEntries match {
        case Failure(_) => logger.error(s"Failed to decode $entries")
        case _ =>
      }
      triedEntries must beSuccessfulTry(entries)
    }
  }

}

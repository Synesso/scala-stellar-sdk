package stellar.sdk.model.ledger

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification

import scala.util.{Failure, Try}

class LedgerEntrySpec extends Specification with LedgerEntryGenerators with LazyLogging {

  "a ledger entry" should {
    "serde to/from XDR" >> prop { entry: LedgerEntry =>
      val triedEntry = Try(LedgerEntry.decode.run(entry.encode.toArray[Byte]).value._2)
      triedEntry match {
        case Failure(_) => logger.error(s"Failed to decode $entry")
        case _ =>
      }
      triedEntry must beSuccessfulTry(entry)
    }
  }

}

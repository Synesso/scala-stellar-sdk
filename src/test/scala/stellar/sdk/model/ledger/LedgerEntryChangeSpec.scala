package stellar.sdk.model.ledger

import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification

import scala.util.{Failure, Try}

class LedgerEntryChangeSpec extends Specification with LedgerEntryGenerators with LazyLogging {

  "a ledger entry change" should {
    "serde to/from XDR" >> prop { change: LedgerEntryChange =>
      val triedChange = Try(LedgerEntryChange.decode.run(change.encode.toArray[Byte]).value._2)
      triedChange match {
        case Failure(_) => logger.error(s"Failed to decode $change")
        case _ =>
      }
      triedChange must beSuccessfulTry(change)
    }
  }

}

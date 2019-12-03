package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.ledger.{LedgerEntryChange, TransactionLedgerEntries}
import stellar.sdk.model.{Desc, Now}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class TransactionLedgerEntriesSpec(ee: ExecutionEnv) extends Specification {

  import ee.ec

  val last100 = Await.result(PublicNetwork.transactions(cursor = Now, order = Desc), 1.minute).take(100).toList

  "transaction meta parsing" should {
    "parse the last 100 without any issues" >> {
      Try {
        last100.map(_.ledgerEntries)
      } must beSuccessfulTry[Seq[TransactionLedgerEntries]]
    }
  }

  "fee ledger entry parsing" should {
    "parse the last 100 without any issues" >> {
      Try {
        last100.flatMap(_.feeLedgerEntries)
      } must beSuccessfulTry[Seq[LedgerEntryChange]]
    }
  }
}

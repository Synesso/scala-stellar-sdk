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

  "wait, what" >> {
    println("From GAAOQIEA24IKDMTLND2G57FN6MFJ4MNULNLPKUGDAH3XPBOAKDT7SSXQ")
    Await.result(
      TestNetwork.transactionsByAccount(KeyPair.fromAccountId("GAAOQIEA24IKDMTLND2G57FN6MFJ4MNULNLPKUGDAH3XPBOAKDT7SSXQ")),
      1.minute).map(_.transaction(TestNetwork)).foreach(println)
    println("From GBLTATTF5XIUVYPI5XMU7YX5BUUMWGXCFFFXD4XKTOTRMLGCNIWO25CY")
    Await.result(
      TestNetwork.transactionsByAccount(KeyPair.fromAccountId("GBLTATTF5XIUVYPI5XMU7YX5BUUMWGXCFFFXD4XKTOTRMLGCNIWO25CY")),
      1.minute).map(_.transaction(TestNetwork)).foreach(println)
    pending
  }

}

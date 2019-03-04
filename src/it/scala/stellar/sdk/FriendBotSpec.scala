package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.{Account, Transaction}

import scala.concurrent.duration._
import scala.util.Try

class FriendBotSpec(implicit ee: ExecutionEnv) extends Specification {

  "the test network" should {
    "allow account funding via friendbot" >> {
      // #friendbot_example
      val kp = KeyPair.random
      val response = TestNetwork.fund(kp)
      // #friendbot_example

      response.map(_.isSuccess must beTrue).awaitFor(1 minute)
    }

    "be used to serialise a transaction" >> {
      val accn = KeyPair.fromPassphrase("an account")
      val sequence = 1
      val txn = {
        // #new_transaction_example
        implicit val network = TestNetwork
        val account = Account(accn, sequence)
        Transaction(account)
        // #new_transaction_example
      }
      Try(txn.encodeXDR) must beASuccessfulTry[String]
    }
  }

}

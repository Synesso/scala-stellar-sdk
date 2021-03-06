package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.op.AccountMergeOperation
import stellar.sdk.model.{Account, AccountId, NativeAmount, TimeBounds, Transaction}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class FriendBotSpec(implicit ee: ExecutionEnv) extends Specification {

  "the test network" should {
    "allow account funding via friendbot" >> {
      // #friendbot_example
      val kp = KeyPair.random
      val response = TestNetwork.fund(kp)
      // #friendbot_example

      val r = Await.result(response, 1 minute)

      // roll it back in, to be a good testnet citizen
      implicit val n = TestNetwork
      val giveItBack = for {
        accn <- n.account(kp)
        friendbot <- response.map(_.transaction.transaction.source)
        response <- Transaction(accn,
          maxFee = NativeAmount(100),
          timeBounds = TimeBounds.Unbounded
        ).add(AccountMergeOperation(friendbot.id)).sign(kp).submit()
      } yield response
      Await.result(giveItBack, 1 minute)

      r.isSuccess must beTrue
    }

    "be used to serialise a transaction" >> {
      val accn = KeyPair.fromPassphrase("an account")
      val sequence = 1
      val txn = {
        // #new_transaction_example
        implicit val network = TestNetwork
        val account = Account(AccountId(accn.publicKey), sequence)
        Transaction(account, maxFee = NativeAmount(100), timeBounds = TimeBounds.Unbounded)
        // #new_transaction_example
      }
      Try(txn.encodeXdrString) must beASuccessfulTry[String]
    }
  }

}

package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.op.{AccountMergeOperation, PaymentOperation}
import stellar.sdk.model.response.TransactionPostResponse
import stellar.sdk.model._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestNetworkJourney(implicit ee: ExecutionEnv) extends Specification {

  implicit val network = TestNetwork

  // TODO - A before/after all that creates a whole chunk of accounts and closes them again.

  "sending a payment" should {
    "be successful" >> {
      val senderKey = KeyPair.random
      val recipientKey = KeyPair.random
      val fundedSender = network.fund(senderKey)
      val fundedRecipient = network.fund(recipientKey)
      val response = for {
        _ <- fundedSender
        _ <- fundedRecipient
        sender <- network.account(senderKey)
        payment = PaymentOperation(recipientKey.toAccountId, Amount.lumens(5000))
        txn = Transaction(sender, List(payment), NoMemo, TimeBounds.Unbounded, Amount.lumens(200))
        response <- txn.sign(senderKey).submit()
      } yield response

      val paymentResponse = Await.result(response, 1.minute)
      Await.result(rollback(fundedSender, senderKey, recipientKey), 20.seconds)
      paymentResponse.isSuccess must beTrue
    }
  }

  private def rollback(response: Future[TransactionPostResponse], keys: KeyPair*): Future[TransactionPostResponse] = {
    for {
      accounts <- Future.sequence(keys.map(network.account))
      friendbot <- response.map(_.transaction.transaction.source)
      operations = keys.map(key => AccountMergeOperation(friendbot.id, Some(key))).toList
      txn = SignedTransaction(
        Transaction(accounts.head, operations, NoMemo, TimeBounds.Unbounded, Amount.lumens(1)),
        Nil
      )
      response <- keys.foldLeft(txn) { case (t, k) => t.sign(k) }.submit()
    } yield response
  }
}

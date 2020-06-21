package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import stellar.sdk.model._
import stellar.sdk.model.op.PaymentOperation
import stellar.sdk.util.ByteArrays

import scala.concurrent.Await
import scala.concurrent.duration._

class TestNetworkJourney(implicit ee: ExecutionEnv) extends Specification with BeforeAfterAll {

  implicit val network = TestNetwork

  private val testAccounts = new TestAccounts(4)

  def beforeAll() = testAccounts.open()
  def afterAll() = testAccounts.close()

  "a client" should {
    "be able to send a payment" >> {
      val List(senderKey, recipientKey) = testAccounts.take(2)
      val response = for {
        sender <- network.account(senderKey)
        payment = PaymentOperation(recipientKey.toAccountId, Amount.lumens(2))
        txn = Transaction(sender, List(payment), NoMemo, TimeBounds.Unbounded, Amount.lumens(1))
        response <- txn.sign(senderKey).submit()
      } yield response

      response.map(_.isSuccess) must beTrue.await(0, 1.minute)
    }

    "be able to fee bump a v0 transaction" >> {
      val List(senderKey, recipientKey) = testAccounts.take(2)
      val sender = Await.result(network.account((senderKey)), 10.seconds)
      val payment = PaymentOperation(recipientKey.toAccountId, Amount.lumens(2))
      val signedTransaction = Transaction(sender, List(payment), NoMemo, TimeBounds.Unbounded, NativeAmount(100))
        .sign(senderKey)
      val parsedV0Txn: SignedTransaction = SignedTransaction.decode.run(signedTransaction.encodeV0).value._2

      val bumpedTxn = parsedV0Txn.bumpFee(NativeAmount(500), recipientKey)
      val response = Await.result(bumpedTxn.submit(), 20.seconds)
      response.isSuccess must beTrue
    }
  }
}

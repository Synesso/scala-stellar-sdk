package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import stellar.sdk.model._
import stellar.sdk.model.op.PaymentOperation

import scala.concurrent.duration._

class TestNetworkJourney(implicit ee: ExecutionEnv) extends Specification with BeforeAfterAll {

  implicit val network = TestNetwork

  private val testAccounts = new TestAccounts()

  def beforeAll() = testAccounts.open()
  def afterAll() = testAccounts.close()

  "a client" should {
    "be able to send a payment" >> {
      val List(senderKey, recipientKey) = testAccounts.take(2)
      val response = for {
        sender <- network.account(senderKey)
        payment = PaymentOperation(recipientKey.toAccountId, Amount.lumens(2))
        txn = Transaction(sender, List(payment), NoMemo, TimeBounds.Unbounded, Amount.lumens(200))
        response <- txn.sign(senderKey).submit()
      } yield response

      response.map(_.isSuccess) must beTrue.await(0, 1.minute)
    }
  }
}

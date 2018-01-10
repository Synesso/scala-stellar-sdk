package stellar.scala.sdk

import org.specs2.mutable.Specification

class PaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "payment operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey, amount: Amount) =>
      val input = PaymentOperation(source, destination, amount)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case po: PaymentOperation =>
          po.destinationAccount.accountId mustEqual destination.accountId
          po.amount must beEquivalentTo(amount)
          po.sourceAccount must beNone
      }
    }
  }

}

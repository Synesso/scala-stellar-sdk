package stellar.scala.sdk

import org.specs2.mutable.Specification

class PathPaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "path payment operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey, amount: Amount) =>
      val input = PaymentOperation(source, destination, amount)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case pa: PaymentOperation =>
          pa.destinationAccount.accountId mustEqual destination.accountId
          pa.amount.units mustEqual amount.units
          pa.amount.asset must beEquivalentTo(amount.asset)
          pa.sourceAccount must beNone
      }
    }
  }

}

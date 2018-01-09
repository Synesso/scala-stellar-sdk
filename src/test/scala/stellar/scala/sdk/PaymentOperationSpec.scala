package stellar.scala.sdk

import org.specs2.mutable.Specification

class PaymentOperationSpec extends Specification with ArbitraryInput {

  "payment operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey, amount: Amount, asset: Asset) =>
      val input = PaymentOperation(source, destination, asset, amount)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case ca: CreateAccountOperation =>
          ca.destinationAccount.accountId mustEqual destination.accountId
          ca.startingBalance mustEqual amount
          ca.sourceAccount must beNone
      }
    }
  }

}

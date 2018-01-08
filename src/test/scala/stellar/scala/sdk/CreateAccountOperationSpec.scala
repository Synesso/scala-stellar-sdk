package stellar.scala.sdk

import org.specs2.mutable.Specification

class CreateAccountOperationSpec extends Specification with ArbitraryInput {

  "create account operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey, amount: Amount) =>
      val input = CreateAccountOperation(source, destination, amount)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case ca: CreateAccountOperation =>
          ca.destinationAccount.accountId mustEqual destination.accountId
          ca.startingBalance mustEqual amount
          ca.sourceAccount must beNone
      }
    }
  }

}

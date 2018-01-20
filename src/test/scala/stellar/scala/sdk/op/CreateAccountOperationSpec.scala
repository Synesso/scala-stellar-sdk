package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk._

class CreateAccountOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "create account operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey, amount: NativeAmount) =>
      val input = CreateAccountOperation(source, destination, amount)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case ca: CreateAccountOperation =>
          ca.destinationAccount.accountId mustEqual destination.accountId
          ca.startingBalance.units mustEqual amount.units
          ca.startingBalance.asset must beEquivalentTo(amount.asset)
          ca.sourceAccount must beNone
      }
    }
  }

}

package stellar.scala.sdk

import org.specs2.mutable.Specification

class AccountMergeOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "account merge operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey) =>
      val input = AccountMergeOperation(destination, Some(source))
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case amo: AccountMergeOperation =>
          amo.destination.accountId mustEqual destination.accountId
          amo.sourceAccount must beNone
      }
    }
  }

}

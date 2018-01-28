package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk.{ArbitraryInput, DomainMatchers, KeyPair, VerifyingKey}

class AccountMergeOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "account merge operation" should {
    "serde via xdr" >> prop { actual: AccountMergeOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: AccountMergeOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

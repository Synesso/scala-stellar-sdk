package stellar.sdk.op

import org.specs2.mutable.Specification
import stellar.sdk.DomainMatchers
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class AccountMergeOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "account merge operation" should {
    "serde via xdr" >> prop { actual: AccountMergeOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: AccountMergeOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

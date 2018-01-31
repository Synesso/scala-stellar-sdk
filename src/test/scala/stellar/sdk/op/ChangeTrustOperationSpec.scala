package stellar.sdk.op

import org.specs2.mutable.Specification
import stellar.sdk.DomainMatchers
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class ChangeTrustOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "change trust operation" should {
    "serde via xdr" >> prop { actual: ChangeTrustOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: ChangeTrustOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk.{Amount, ArbitraryInput, DomainMatchers, KeyPair}

class ChangeTrustOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "change trust operation" should {
    "serde via xdr" >> prop { actual: ChangeTrustOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: ChangeTrustOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

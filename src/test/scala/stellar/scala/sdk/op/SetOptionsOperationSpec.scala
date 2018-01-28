package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk.{ArbitraryInput, DomainMatchers, KeyPair}

class SetOptionsOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "set options operation" should {
    "serde via xdr" >> prop { actual: SetOptionsOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: SetOptionsOperation => expected must beEquivalentTo(actual)
      }
    }
  }
}

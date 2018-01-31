package stellar.sdk.op

import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class PathPaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "path payment operation" should {
    "serde via xdr" >> prop { actual: PathPaymentOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: PathPaymentOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

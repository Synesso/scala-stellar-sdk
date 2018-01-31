package stellar.sdk.op

import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class PaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "payment operation" should {
    "serde via xdr" >> prop { actual: PaymentOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: PaymentOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

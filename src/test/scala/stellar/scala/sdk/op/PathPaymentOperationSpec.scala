package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk._

class PathPaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "path payment operation" should {
    "serde via xdr" >> prop { actual: PathPaymentOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: PathPaymentOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

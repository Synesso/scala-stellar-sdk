package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk._

class PaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "payment operation" should {
    "serde via xdr" >> prop { (actual: PaymentOperation, source: KeyPair) =>
      Operation.fromXDR(actual.toXDR(source)) must beSuccessfulTry.like {
        case expected: PaymentOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk._

class CreatePassiveOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "create passive offer operation" should {
    "serde via xdr" >> prop { actual: CreatePassiveOfferOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: CreatePassiveOfferOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

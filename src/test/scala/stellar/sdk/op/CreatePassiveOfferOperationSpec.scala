package stellar.sdk.op

import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class CreatePassiveOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "create passive offer operation" should {
    "serde via xdr" >> prop { actual: CreatePassiveOfferOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: CreatePassiveOfferOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

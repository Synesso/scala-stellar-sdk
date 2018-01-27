package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk._

class CreatePassiveOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "create passive offer operation" should {
    "serde via xdr" >> prop { (actual: CreatePassiveOfferOperation, source: KeyPair) =>
      Operation.fromXDR(actual.toXDR(source)) must beSuccessfulTry.like {
        case expected: CreatePassiveOfferOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

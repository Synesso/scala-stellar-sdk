package stellar.scala.sdk.op

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.ManageOfferOp
import stellar.scala.sdk._

class ManageOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "create offer operation" should {
    "serde via xdr" >> prop { actual: CreateOfferOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: CreateOfferOperation => expected must beEquivalentTo(actual)
      }
    }
  }

  "update offer operation" should {
    "serde via xdr" >> prop { actual: UpdateOfferOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: UpdateOfferOperation => expected must beEquivalentTo(actual)
      }
    }
  }

  "delete offer operation" should {
    "serde via xdr" >> prop { actual: DeleteOfferOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: DeleteOfferOperation => expected must beEquivalentTo(actual)
      }
    }
  }

  "manage offer op with no id and no details" should {
    "not deserialise" >> {
      ManageOfferOperation.from(new ManageOfferOp) must beFailedTry[ManageOfferOperation]
    }
  }

}

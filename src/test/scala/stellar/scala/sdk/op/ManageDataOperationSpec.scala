package stellar.scala.sdk.op

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.scala.sdk.{ArbitraryInput, DomainMatchers, KeyPair}

class ManageDataOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a write data operation" should {
    "serde via xdr" >> prop { (actual: WriteDataOperation, source: KeyPair) =>
      Operation.fromXDR(actual.toXDR(source)) must beSuccessfulTry.like {
        case expected: WriteDataOperation => expected must beEquivalentTo(actual)
      }
    }
  }

  "a delete data operation" should {
    "serde via xdr" >> prop { (actual: DeleteDataOperation, source: KeyPair) =>
      Operation.fromXDR(actual.toXDR(source)) must beSuccessfulTry.like {
        case expected: DeleteDataOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

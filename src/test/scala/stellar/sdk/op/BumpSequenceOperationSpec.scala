package stellar.sdk.op

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class BumpSequenceOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  "bump sequence operation" should {
    "serde via xdr" >> prop { actual: BumpSequenceOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: BumpSequenceOperation => expected must beEquivalentTo(actual)
      }
    }

  }

}

package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk.{Amount, ArbitraryInput, DomainMatchers, KeyPair}

class ChangeTrustOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "change trust operation" should {
    "serde via xdr" >> prop { (source: KeyPair, limit: Amount) =>
      val input = ChangeTrustOperation(source, limit)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case cto: ChangeTrustOperation =>
          cto.limit must beEquivalentTo(limit)
          cto.sourceAccount must beNone
      }
    }
  }

}

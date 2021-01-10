package stellar.sdk.model.result

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class OperationResultSpec extends Specification with ArbitraryInput with DomainMatchers {
  "operation results" should {
    "serde via xdr bytes" >> prop { or: OperationResult =>
      OperationResult.decode(or.xdr) mustEqual or
    }
  }
}

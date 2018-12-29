package stellar.sdk.res

import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class OperationResultSpec extends Specification with ArbitraryInput {

  "operation results" should {
    "serde via xdr bytes" >> prop { or: OperationResult =>
      val (remaining, decoded) = OperationResult.decode.run(or.encode).value
      decoded mustEqual or
      remaining must beEmpty
    }
  }

}

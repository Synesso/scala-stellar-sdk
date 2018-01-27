package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk.{ArbitraryInput, KeyPair}

class InflationOperationSpec extends Specification with ArbitraryInput {

  "the inflation operation" should {
    "serde via xdr" >> prop { source: KeyPair =>
      Operation.fromXDR(InflationOperation.toXDR(source)) must beSuccessfulTry[Operation](InflationOperation)
    }
  }

}

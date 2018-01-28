package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk.{ArbitraryInput, KeyPair}

class InflationOperationSpec extends Specification {

  "the inflation operation" should {
    "serde via xdr" >> {
      Operation.fromXDR(InflationOperation.toXDR) must beSuccessfulTry[Operation](InflationOperation)
    }
  }

}

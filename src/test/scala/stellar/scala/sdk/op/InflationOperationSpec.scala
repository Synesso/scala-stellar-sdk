package stellar.scala.sdk.op

import org.specs2.mutable.Specification

class InflationOperationSpec extends Specification {

  "the inflation operation" should {
    "serde via xdr" >> {
      Operation.fromXDR(InflationOperation.toXDR) must beSuccessfulTry[Operation](InflationOperation)
    }
  }

}

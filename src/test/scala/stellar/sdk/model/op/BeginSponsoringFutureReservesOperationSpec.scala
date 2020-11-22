package stellar.sdk.model.op

import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.util.ByteArrays

class BeginSponsoringFutureReservesOperationSpec extends Specification with ArbitraryInput {

  implicit val arbOp: Arbitrary[BeginSponsoringFutureReservesOperation] =
    Arbitrary(genBeginSponsoringFutureReservesOperation)

  "begin sponsoring future reserves operation" should {
    "serde via xdr bytes" >> prop { actual: BeginSponsoringFutureReservesOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "serde via xdr string" >> prop { actual: BeginSponsoringFutureReservesOperation =>
      Operation.decodeXDR(ByteArrays.base64(actual.encode)) mustEqual actual
    }
  }

}

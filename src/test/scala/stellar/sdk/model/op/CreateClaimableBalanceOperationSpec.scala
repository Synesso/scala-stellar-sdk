package stellar.sdk.model.op

import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.util.ByteArrays

class CreateClaimableBalanceOperationSpec extends Specification with ArbitraryInput {

  implicit val arbOp: Arbitrary[CreateClaimableBalanceOperation] = Arbitrary(genCreateClaimableBalanceOperation)
  implicit val arbTx: Arbitrary[Transacted[CreateClaimableBalanceOperation]] =
    Arbitrary(genTransacted(genCreateClaimableBalanceOperation))

  "create claimable balance operation" should {
    "serde via xdr bytes" >> prop { actual: CreateClaimableBalanceOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "serde via xdr string" >> prop { actual: CreateClaimableBalanceOperation =>
      Operation.decodeXDR(ByteArrays.base64(actual.encode)) mustEqual actual
    }

  }
}

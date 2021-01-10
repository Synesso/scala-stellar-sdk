package stellar.sdk.model

import okio.ByteString
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.ClaimableBalanceIds.genClaimableBalanceId

class ClaimableBalanceIdSpec extends Specification with ArbitraryInput {

  implicit val arbClaimableBalanceId: Arbitrary[ClaimableBalanceId] = Arbitrary(genClaimableBalanceId)

  "claimable balance hash id" should {
    "serde via xdr" >> prop { actual: ClaimableBalanceId =>
      ClaimableBalanceId.decode(actual.xdr) mustEqual actual
    }
  }
}

object ClaimableBalanceIds {
  val genClaimableBalanceId: Gen[ClaimableBalanceId] =
    Gen.containerOfN[Array, Byte](32, Gen.posNum[Byte]).map(new ByteString(_)).map(ClaimableBalanceHashId)
}

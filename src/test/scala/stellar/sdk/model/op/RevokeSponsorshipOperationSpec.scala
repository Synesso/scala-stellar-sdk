package stellar.sdk.model.op

import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class RevokeSponsorshipOperationSpec extends Specification with ArbitraryInput {

  implicit val arbOp: Arbitrary[RevokeSponsorshipOperation] =
    Arbitrary(genRevokeSponsorshipOperation)

  "revoke sponsorship operation" should {
    "serde via xdr" >> prop { actual: RevokeSponsorshipOperation =>
      Operation.decode(actual.xdr) mustEqual actual
    }
  }

}

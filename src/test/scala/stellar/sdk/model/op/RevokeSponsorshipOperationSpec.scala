package stellar.sdk.model.op

import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class RevokeSponsorshipOperationSpec extends Specification with ArbitraryInput {

  implicit val arbOp: Arbitrary[RevokeSponsorshipOperation] =
    Arbitrary(genRevokeSponsorshipOperation)

  "revoke sponsorship operation" should {
    "serde via xdr bytes" >> prop { actual: RevokeSponsorshipOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "serde via xdr string" >> prop { actual: RevokeSponsorshipOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) mustEqual actual
    }
  }

}

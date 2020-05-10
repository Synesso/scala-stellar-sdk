package stellar.sdk.model

import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class AccountSpec extends Specification with ArbitraryInput {

  "an account" should {
    "provide the successive version of itself" >> prop { account: Account =>
      val next = account.withIncSeq
      next.sequenceNumber mustEqual account.sequenceNumber + 1
      next.id mustEqual account.id
    }
  }

}

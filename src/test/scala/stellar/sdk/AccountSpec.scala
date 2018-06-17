package stellar.sdk

import org.specs2.mutable.Specification

class AccountSpec extends Specification with ArbitraryInput {

  "an account" should {
    "provide the successive version of itself" >> prop { account: Account =>
      val next = account.withIncSeq
      next.sequenceNumber mustEqual account.sequenceNumber + 1
      next.publicKey mustEqual account.publicKey
    }
  }

}

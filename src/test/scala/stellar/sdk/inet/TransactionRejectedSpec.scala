package stellar.sdk.inet

import org.specs2.mutable.Specification
import stellar.sdk.res.TransactionRejected

class TransactionRejectedSpec extends Specification {

  "failure" should {
    "decode any result XDR" >> {
      TransactionRejected(1, "", "", Nil, "", "AAAAAAAAAGT////9AAAAAA==").result must not(beNull)
    }
  }

}

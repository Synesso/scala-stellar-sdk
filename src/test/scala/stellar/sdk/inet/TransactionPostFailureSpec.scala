package stellar.sdk.inet

import org.specs2.mutable.Specification
import stellar.sdk.resp.TransactionPostFailure

class TransactionPostFailureSpec extends Specification {

  "failure" should {
    "decode any result XDR" >> {
      TransactionPostFailure(1, "", "", "AAAAAAAAAGT////9AAAAAA==", "", Array.empty[String]).result must not(beNull)
    }
  }

}

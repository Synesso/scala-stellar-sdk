package stellar.sdk.inet

import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.TransactionResult

class TxnFailureSpec extends Specification {

  "failure" should {
    "decode any result XDR" >> {
      TxnFailure(null, 1, "", None, None, Some("AAAAAAAAAGT////9AAAAAA==")).result must beSome[TransactionResult]
    }
  }

}

package stellar.sdk.model.result

import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.NativeAmount
import stellar.sdk.model.response.TransactionRejected

class TransactionRejectedSpec extends Specification with ArbitraryInput {

  "an approved transaction result" should {
    "provide direct access to the fee charged" >> prop { result: TransactionNotSuccessful =>
      val resultXDR = result.xdr.encode().base64()
      TransactionRejected(400, "", "", Nil, "", resultXDR).feeCharged mustEqual result.feeCharged
    }

    "indicate whether the sequence was updated based upon presence of fee" >> prop { result: TransactionNotSuccessful =>
      val xdr = result.xdr
      val resultXDR = xdr.encode().base64()
      val rejection = TransactionRejected(400, "", "", Nil, "", resultXDR)
      rejection.sequenceIncremented mustEqual result.feeCharged != NativeAmount(0)
    }
  }

  "failure" should {
    "decode any result XDR" >> {
      TransactionRejected(1, "", "", Nil, "", "AAAAAAAAAGT////9AAAAAA==").result must not(beNull)
    }
  }
}

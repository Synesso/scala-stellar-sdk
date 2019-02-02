package stellar.sdk.model.result

import org.specs2.mutable.Specification
import stellar.sdk.model.response.TransactionRejected
import stellar.sdk.util.ByteArrays
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.NativeAmount

class TransactionRejectedSpec extends Specification with ArbitraryInput {

  "an approved transaction result" should {
    "provide direct access to the fee charged" >> prop { result: TransactionNotSuccessful =>
      val resultXDR = ByteArrays.base64(result.encode)
      TransactionRejected(400, "", "", Nil, "", resultXDR).feeCharged mustEqual result.feeCharged
    }

    "indicate whether the sequence was updated based upon presence of fee" >> prop { result: TransactionNotSuccessful =>
      val resultXDR = ByteArrays.base64(result.encode)
      val rejection = TransactionRejected(400, "", "", Nil, "", resultXDR)
      rejection.sequenceIncremented mustEqual result.feeCharged != NativeAmount(0)
    }
  }
}

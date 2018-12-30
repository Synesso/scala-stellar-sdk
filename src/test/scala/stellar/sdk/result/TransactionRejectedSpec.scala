package stellar.sdk.result

import org.specs2.mutable.Specification
import stellar.sdk.response.TransactionRejected
import stellar.sdk.{ArbitraryInput, ByteArrays, NativeAmount}

class TransactionRejectedSpec extends Specification with ArbitraryInput {

  "an approved transaction result" should {
    "provide direct access to the fee charged" >> prop { result: TransactionNotSuccessful =>
      val resultXDR = ByteArrays.base64(result.encode)
      TransactionRejected(400, "", "", Nil, "", resultXDR).feeCharged must beLike[NativeAmount] { case amnt =>
        result match {
          case TransactionFailure(fee, _) => amnt mustEqual fee
          case _ => amnt mustEqual NativeAmount(0)
        }
      }
    }
  }


}

package stellar.sdk.model.result

import okio.ByteString
import org.specs2.mutable.Specification
import stellar.sdk.model.response.TransactionApproved
import stellar.sdk.util.ByteArrays
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.NativeAmount

class TransactionApprovedSpec extends Specification with ArbitraryInput {

  "an approved transaction result" should {
    "provide direct access to the fee charged" >> {
      val resultXDR = TransactionSuccess(NativeAmount(982346), Seq(PaymentSuccess), ByteString.EMPTY).xdr.encode().base64()
      TransactionApproved("", 1, "", resultXDR, "").feeCharged mustEqual NativeAmount(982346)
    }

    "provide direct access to the operation results" >> prop { opResults: List[OperationResult] =>
      val xdr = TransactionSuccess(NativeAmount(100), opResults.take(20), ByteString.EMPTY).xdr
      val resultXDR = xdr.encode().base64()

      TransactionApproved("", 1, "", resultXDR, "").operationResults mustEqual opResults.take(20)
    }
  }

}

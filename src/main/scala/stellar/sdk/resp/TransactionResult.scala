package stellar.sdk.resp

import org.stellar.sdk.xdr.{TransactionResult => XDRTransactionResult}
import stellar.sdk.{NativeAmount, XDRPrimitives}

import scala.util.Try

sealed trait TransactionResult {
  val feeCharged: NativeAmount
}

case class TransactionSuccess(feeCharged: NativeAmount) extends TransactionResult
case class TransactionFailure(feeCharged: NativeAmount) extends TransactionResult

object TransactionResult {

  def decodeXDR(base64: String): Try[TransactionResult] = {
    Try(XDRTransactionResult.decode(XDRPrimitives.inputStream(base64))).map { tr =>
      TransactionSuccess(
        feeCharged = NativeAmount(tr.getFeeCharged.getInt64)
      )
    }
  }

}

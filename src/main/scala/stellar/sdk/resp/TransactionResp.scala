package stellar.sdk.resp

import java.io.ByteArrayInputStream

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import org.stellar.sdk.xdr.{OperationResult, TransactionResult, XdrDataInputStream}
import stellar.sdk.NativeAmount

import scala.util.Try

sealed trait TransactionResp {
  val feeCharged: NativeAmount
}

case class TxnSuccess(feeCharged: NativeAmount) extends TransactionResp
case class TxnFailed(feeCharged: NativeAmount) extends TransactionResp
case class TxnTooEarly(feeCharged: NativeAmount) extends TransactionResp
case class TxnTooLate(feeCharged: NativeAmount) extends TransactionResp
case class TxnMissingOperation(feeCharged: NativeAmount) extends TransactionResp
case class TxnBadSequence(feeCharged: NativeAmount) extends TransactionResp
case class TxnBadAuth(feeCharged: NativeAmount) extends TransactionResp
case class TxnInsufficientBalance(feeCharged: NativeAmount) extends TransactionResp
case class TxnNoAccount(feeCharged: NativeAmount) extends TransactionResp
case class TxnInsufficientFee(feeCharged: NativeAmount) extends TransactionResp
case class TxnBadAuthExtra(feeCharged: NativeAmount) extends TransactionResp
case class TxnInternalError(feeCharged: NativeAmount) extends TransactionResp

object TransactionResp {
  def from(tr: TransactionResult): Try[TransactionResp] = Try {
    val fee = NativeAmount(tr.getFeeCharged.getInt64)
    tr.getResult.getResults.toSeq.map {
      case or: OperationResult => or.getTr.getDiscriminant
    }
    tr.getResult.getDiscriminant match {
      case _ => ???
      /*
      TransactionResultCode.txSUCCESS =>
       */
    }
    TxnFailed(fee)
  }
}

object TransactionRespDeserializer extends CustomSerializer[TransactionResp](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats
  TransactionResp.from({
    val is = new ByteArrayInputStream((o \ "result_xdr").extract[String].getBytes("UTF-8"))
    TransactionResult.decode(new XdrDataInputStream(is))
  }).get
}, PartialFunction.empty)
)


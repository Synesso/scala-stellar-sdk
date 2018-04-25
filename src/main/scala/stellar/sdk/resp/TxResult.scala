package stellar.sdk.resp

import org.stellar.sdk.xdr.TransactionResult.TransactionResultResult
import org.stellar.sdk.xdr.{ TransactionResultCode, XdrDataOutputStream, TransactionResult => XDRTransactionResult}
import stellar.sdk.{NativeAmount, XDRPrimitives}

import scala.util.Try

sealed trait TxResult {
  val feeCharged: NativeAmount
}

/**
  * The transaction was successful
  */
case class TxSuccess(feeCharged: NativeAmount) extends TxResult

/**
  * One of the operations failed (none were applied)
  */
case class TxFailBadOperation(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction had not yet entered its valid time window
  */
case class TxFailTooEarly(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction's valid time window has already passed
  */
case class TxFailTooLate(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction didn't have any operations
  */
case class TxFailMissingOperation(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction did not have the correct sequence number
  */
case class TxFailBadSequence(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction had too few valid signatures for the given network
  */
case class TxFailBadSignatures(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction would have reduced an account balance below the reserve
  */
case class TxFailInsufficientBalance(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction did not specify a source account
  */
case class TxFailNoSourceAccount(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction did not include sufficient fees
  */
case class TxFailInsufficientFee(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction included extraneous signatures
  */
case class TxFailUnusedSignatures(feeCharged: NativeAmount) extends TxResult

/**
  * The transaction failed for an unknown reason
  */
case class TxFailOther(code: Int, feeCharged: NativeAmount) extends TxResult


object TxResult {

  def decodeXDR(base64: String): Try[TxResult] = {
    Try(XDRTransactionResult.decode(XDRPrimitives.inputStream(base64))).map { tr =>
      val fee = NativeAmount(tr.getFeeCharged.getInt64)
      tr.getResult.getDiscriminant match {
        case TransactionResultCode.txSUCCESS => TxSuccess(fee)
        case TransactionResultCode.txFAILED => TxFailBadOperation(fee)
        case TransactionResultCode.txTOO_EARLY => TxFailTooEarly(fee)
        case TransactionResultCode.txTOO_LATE => TxFailTooLate(fee)
        case TransactionResultCode.txBAD_AUTH => TxFailBadSignatures(fee)
        case TransactionResultCode.txMISSING_OPERATION => TxFailMissingOperation(fee)
        case TransactionResultCode.txINSUFFICIENT_BALANCE => TxFailInsufficientBalance(fee)
        case TransactionResultCode.txINSUFFICIENT_FEE => TxFailInsufficientFee(fee)
        case TransactionResultCode.txNO_ACCOUNT => TxFailNoSourceAccount(fee)
        case TransactionResultCode.txBAD_AUTH_EXTRA => TxFailUnusedSignatures(fee)
        case TransactionResultCode.txBAD_SEQ => TxFailBadSequence(fee)
        case code => TxFailOther(code.getValue, fee)
      }
    }
  }
}

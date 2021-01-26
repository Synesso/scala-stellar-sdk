package stellar.sdk.model.result

import okio.ByteString
import org.stellar.xdr.InnerTransactionResult.InnerTransactionResultResult
import org.stellar.xdr.{Hash, InnerTransactionResult, InnerTransactionResultPair, Int64, TransactionResultCode, TransactionResult => XTransactionResult}
import stellar.sdk.model.NativeAmount
import stellar.sdk.model.result.TransactionResult.Code

sealed trait TransactionResult {
  val isSuccess: Boolean
  def sequenceUpdated: Boolean
  def xdr: XTransactionResult
}

/**
  * The transaction and all contained operations were successfully processed.
  */
case class TransactionSuccess(
  feeCharged: NativeAmount,
  operationResults: Seq[OperationResult],
  hash: ByteString
) extends TransactionResult {
  val isSuccess: Boolean = true
  def sequenceUpdated: Boolean = true

  override def xdr: XTransactionResult = new XTransactionResult.Builder()
    .feeCharged(new Int64(feeCharged.units))
    .result(new XTransactionResult.TransactionResultResult.Builder()
      .discriminant(TransactionResultCode.txSUCCESS)
      .innerResultPair(new InnerTransactionResultPair.Builder()
        .result(new InnerTransactionResult.Builder()
          .feeCharged(new Int64(feeCharged.units))
          .result(new InnerTransactionResultResult.Builder()
            .discriminant(TransactionResultCode.txSUCCESS)
            .results(operationResults.map(_.xdr).toArray)
            .build())
          .ext(new InnerTransactionResult.InnerTransactionResultExt.Builder()
            .discriminant(0)
            .build())
          .build())
        .transactionHash(new Hash(hash.toByteArray))
        .build())
      .results(operationResults.map(_.xdr).toArray)
      .build())
    .ext(new XTransactionResult.TransactionResultExt.Builder()
      .discriminant(0)
      .build())
    .build()
}

sealed trait TransactionNotSuccessful extends TransactionResult {
  val feeCharged: NativeAmount
  val isSuccess: Boolean = false
  def sequenceUpdated: Boolean = feeCharged.units != 0
}


/**
  * The transaction failed when processing the operations.
  */
case class TransactionFailure(feeCharged: NativeAmount, operationResults: Seq[OperationResult]) extends TransactionNotSuccessful {
  override def xdr: XTransactionResult = new XTransactionResult.Builder()
    .feeCharged(new Int64(feeCharged.units))
    .result(new XTransactionResult.TransactionResultResult.Builder()
      .discriminant(TransactionResultCode.txFAILED)
      .results(operationResults.map(_.xdr).toArray)
      .build())
    .ext(new XTransactionResult.TransactionResultExt.Builder()
      .discriminant(0)
      .build())
    .build()
}

/**
  * The transaction failed for the reason given prior to any operations being attempted.
  */
case class TransactionNotAttempted(reason: TransactionResultCode, feeCharged: NativeAmount) extends TransactionNotSuccessful {
  override def xdr: XTransactionResult = new XTransactionResult.Builder()
    .feeCharged(new Int64(feeCharged.units))
    .result(new XTransactionResult.TransactionResultResult.Builder()
      .discriminant(reason)
      .build())
    .ext(new XTransactionResult.TransactionResultExt.Builder()
      .discriminant(0)
      .build())
    .build()
}


object TransactionResult {
  def decodeXDR(resultXDR: String): TransactionResult = ???

  sealed abstract class Code(val id: Int) {
  }

  sealed trait OperationsNotAttempted {
    this: Code =>
    val id: Int
  }

  case object Successful extends Code(0)
  case object OperationsFailed extends Code(-1)
  case object SubmittedTooEarly extends Code(-2) with OperationsNotAttempted
  case object SubmittedTooLate extends Code(-3) with OperationsNotAttempted
  case object NoOperations extends Code(-4) with OperationsNotAttempted
  case object BadSequenceNumber extends Code(-5) with OperationsNotAttempted
  case object BadAuthorisation extends Code(-6) with OperationsNotAttempted
  case object InsufficientBalance extends Code(-7) with OperationsNotAttempted
  case object SourceAccountNotFound extends Code(-8) with OperationsNotAttempted
  case object InsufficientFee extends Code(-9) with OperationsNotAttempted
  case object UnusedSignatures extends Code(-10) with OperationsNotAttempted
  case object UnspecifiedInternalError extends Code(-11) with OperationsNotAttempted

  object Code {
    def apply(i: Int): Code = i match {
      case 0 => Successful
      case -1 => OperationsFailed
      case -2 => SubmittedTooEarly
      case -3 => SubmittedTooLate
      case -4 => NoOperations
      case -5 => BadSequenceNumber
      case -6 => BadAuthorisation
      case -7 => InsufficientBalance
      case -8 => SourceAccountNotFound
      case -9 => InsufficientFee
      case -10 => UnusedSignatures
      case -11 => UnspecifiedInternalError
      case _ => sys.error(s"TransactionResult code $i is unknown")
    }
  }
}

package stellar.sdk.model.result

import stellar.sdk.model.NativeAmount
import stellar.sdk.model.result.TransactionResult.Code

sealed trait TransactionResult {
  val isSuccess: Boolean
  def sequenceUpdated: Boolean
}

/**
  * The transaction and all contained operations were successfully processed.
  */
case class TransactionSuccess(feeCharged: NativeAmount, operationResults: Seq[OperationResult]) extends TransactionResult {
  val isSuccess: Boolean = true

  def sequenceUpdated: Boolean = true
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
}

/**
  * The transaction failed for the reason given prior to any operations being attempted.
  */
case class TransactionNotAttempted(reason: Code, feeCharged: NativeAmount) extends TransactionNotSuccessful {
  val resultCode: Int = reason.id
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

package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.NativeAmount
import stellar.sdk.model.result.TransactionResult.NotAttempted
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays

sealed trait TransactionResult extends Encodable {
  val isSuccess: Boolean
}

/**
  * The transaction and all contained operations were successfully processed.
  */
case class TransactionSuccess(feeCharged: NativeAmount, operationResults: Seq[OperationResult]) extends TransactionResult {
  val isSuccess: Boolean = true

  def encode: Stream[Byte] =
    Encode.long(feeCharged.units) ++
      Encode.int(0) ++
      Encode.arr(operationResults) ++
      Encode.int(0)
}

sealed trait TransactionNotSuccessful extends TransactionResult {
  val isSuccess: Boolean = false
}

/**
  * The transaction was attempted, but one or more operations failed (and none were applied).
  */
case class TransactionFailure(feeCharged: NativeAmount, operationResults: Seq[OperationResult]) extends TransactionNotSuccessful {

  def encode: Stream[Byte] =
    Encode.long(feeCharged.units) ++
      Encode.int(-1) ++
      Encode.arr(operationResults) ++
      Encode.int(0)
}

/**
  * The transaction was not attempted for the reason given.
  */
case class TransactionNotAttempted(reason: NotAttempted, feeCharged: NativeAmount) extends TransactionNotSuccessful {

  val resultCode: Int = reason.id

  def encode: Stream[Byte] =
      Encode.long(feeCharged.units) ++
      Encode.int(reason.id) ++
      Encode.int(0)
}


object TransactionResult {

  def decodeXDR(base64: String) = decode.run(ByteArrays.base64(base64)).value._2

  def decode: State[Seq[Byte], TransactionResult] = for {
    feeCharged <- Decode.long.map(NativeAmount)
    result <- Code.decode
    operationResults <- result.decodeOperationResults
    _ <- Decode.int
  } yield result match {
    case Successful => TransactionSuccess(feeCharged, operationResults)
    case OperationsFailed => TransactionFailure(feeCharged, operationResults)
    case reason: NotAttempted => TransactionNotAttempted(reason, feeCharged)
  }

  sealed abstract class Code(val id: Int) {
    val decodeOperationResults: State[Seq[Byte], Seq[OperationResult]] = Decode.arr(OperationResult.decode)
  }

  sealed trait NotAttempted {
    this: Code =>
    val id: Int
    override val decodeOperationResults: State[Seq[Byte], Seq[OperationResult]] = State.pure(Nil)
  }

  case object Successful extends Code(0)
  case object OperationsFailed extends Code(-1)
  case object SubmittedTooEarly extends Code(-2) with NotAttempted
  case object SubmittedTooLate extends Code(-3) with NotAttempted
  case object NoOperations extends Code(-4) with NotAttempted
  case object BadSequenceNumber extends Code(-5) with NotAttempted
  case object BadAuthorisation extends Code(-6) with NotAttempted
  case object InsufficientBalance extends Code(-7) with NotAttempted
  case object SourceAccountNotFound extends Code(-8) with NotAttempted
  case object InsufficientFee extends Code(-9) with NotAttempted
  case object UnusedSignatures extends Code(-10) with NotAttempted
  case object UnspecifiedInternalError extends Code(-11) with NotAttempted

  object Code {
    def decode: State[Seq[Byte], Code] = Decode.int.map(apply)
    
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

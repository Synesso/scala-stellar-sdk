package stellar.sdk.result

import cats.data.State
import stellar.sdk.xdr.{Decode, Encodable, Encode}

/**
  * The result of an operation previously submitted to the network.
  */
abstract class OperationResult(opCode: Int) extends Encodable {
  def encode: Stream[Byte] = Encode.int(opCode)
}

/**
  * The result of an operation previously submitted to, and attempted to be processed by the network.
  */
abstract class ProcessedOperationResult(opCode: Int) extends OperationResult(opCode) {
  val opResultCode: Int
  override def encode: Stream[Byte] =
    Encode.int(0) ++ // operation was attempted
      super.encode ++ // the operation code (positive only)
      Encode.int(opResultCode) // the operation result code
}

object OperationResult {

  private def widen[A, W, O <: W](s: State[A, O]): State[A, W] = s.map(w => w: W)

  val decode: State[Seq[Byte], OperationResult] = Decode.int.flatMap {
    case -3 => widen(State.pure(OperationNotSupportedResult))
    case -2 => widen(State.pure(NoSourceAccountResult))
    case -1 => widen(State.pure(BadAuthenticationResult))
    case 0 => Decode.int.flatMap {
      case 0 => widen(CreateAccountResult.decode)
      case 1 => widen(PaymentResult.decode)
      case 2 => widen(PathPaymentResult.decode)
      case 3 => widen(ManageOfferResult.decode)
      case 4 => widen(CreatePassiveOfferResult.decode)
      case 5 => widen(SetOptionsResult.decode)
      case 6 => widen(ChangeTrustResult.decode)
      case 7 => widen(AllowTrustResult.decode)
      case 8 => widen(AccountMergeResult.decode)
      case 9 => widen(InflationResult.decode)
      case 10 => widen(ManageDataResult.decode)
      case 11 => widen(BumpSequenceResult.decode)
    }
  }
}

/**
  * The operation was not attempted, because there were too few valid signatures, or the wrong network was used.
  */
case object BadAuthenticationResult extends OperationResult(-1)

/**
  * The operation was not attempted, because the source account was not found.
  */
case object NoSourceAccountResult extends OperationResult(-2)

/**
  * The operation was not attempted, because the requested operation is not supported by the network.
  */
case object OperationNotSupportedResult extends OperationResult(-3)
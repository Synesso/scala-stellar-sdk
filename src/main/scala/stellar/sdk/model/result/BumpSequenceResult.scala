package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.xdr.Decode

sealed abstract class BumpSequenceResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 11)

object BumpSequenceResult {
  val decode: State[Seq[Byte], BumpSequenceResult] = Decode.int.map {
    case 0 => BumpSequenceSuccess
    case -1 => BumpSequenceBadSeqNo
  }
}

/**
  * BumpSequence operation was successful.
  */
case object BumpSequenceSuccess extends BumpSequenceResult(0)

/**
  * BumpSequence operation failed because the desired sequence number was less than zero.
  */
case object BumpSequenceBadSeqNo extends BumpSequenceResult(-1)


package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{BumpSequenceResultCode, OperationType}

sealed abstract class BumpSequenceResult extends ProcessedOperationResult {
  val result: org.stellar.xdr.BumpSequenceResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.BUMP_SEQUENCE)
    .bumpSeqResult(result)
    .build()
}

object BumpSequenceResult {
}

/**
  * BumpSequence operation was successful.
  */
case object BumpSequenceSuccess extends BumpSequenceResult {
  val result: org.stellar.xdr.BumpSequenceResult = new org.stellar.xdr.BumpSequenceResult.Builder()
    .discriminant(BumpSequenceResultCode.BUMP_SEQUENCE_SUCCESS)
    .build()
}

/**
  * BumpSequence operation failed because the desired sequence number was not within valid bounds.
  */
case object BumpSequenceBadSeqNo extends BumpSequenceResult {
  val result: org.stellar.xdr.BumpSequenceResult = new org.stellar.xdr.BumpSequenceResult.Builder()
    .discriminant(BumpSequenceResultCode.BUMP_SEQUENCE_BAD_SEQ)
    .build()
}


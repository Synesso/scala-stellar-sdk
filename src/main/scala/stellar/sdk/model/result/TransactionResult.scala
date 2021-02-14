package stellar.sdk.model.result

import okio.ByteString
import org.stellar.xdr.InnerTransactionResult.InnerTransactionResultResult
import org.stellar.xdr.TransactionResultCode.{txFEE_BUMP_INNER_FAILED, txFEE_BUMP_INNER_SUCCESS}
import org.stellar.xdr.{Hash, InnerTransactionResult, InnerTransactionResultPair, Int64, TransactionResultCode, TransactionResult => XTransactionResult}
import stellar.sdk.model.NativeAmount

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
 *
 * If the reasons is fee bump related, then innerTransaction must be defined. Otherwise not.
 */
case class TransactionNotAttempted(
  reason: TransactionResultCode,
  feeCharged: NativeAmount,
  innerTransaction: Option[FeeBumpedTransactionResult]
) extends TransactionNotSuccessful {
  require(innerTransaction match {
    case None => reason != txFEE_BUMP_INNER_SUCCESS && reason != txFEE_BUMP_INNER_FAILED
    case _ => reason == txFEE_BUMP_INNER_SUCCESS || reason == txFEE_BUMP_INNER_FAILED
  }, "Only define innerTransaction when reason is fee bump related")

  override def xdr: XTransactionResult = {
    new XTransactionResult.Builder()
      .feeCharged(new Int64(feeCharged.units))
      .result(new XTransactionResult.TransactionResultResult.Builder()
        .discriminant(reason)
        .innerResultPair(innerTransaction.map(_.xdr).orNull)
        .build())
      .ext(new XTransactionResult.TransactionResultExt.Builder()
        .discriminant(0)
        .build())
      .build()
  }
}


object TransactionResult {
  def decodeXdrString(xdr: String): TransactionResult = decodeXdr(XTransactionResult.decode(ByteString.decodeBase64(xdr)))
  def decodeXdr(xdr: XTransactionResult): TransactionResult = {
    val fee = NativeAmount(xdr.getFeeCharged.getInt64)
    xdr.getResult.getDiscriminant match {
      case TransactionResultCode.txSUCCESS =>
        TransactionSuccess(
          feeCharged = fee,
          operationResults = xdr.getResult.getResults.map(OperationResult.decodeXdr).toList,
          hash = ByteString.EMPTY
        )

      case TransactionResultCode.txFAILED =>
        TransactionFailure(
          feeCharged = fee,
          operationResults = xdr.getResult.getResults.map(OperationResult.decodeXdr).toList
        )

      case TransactionResultCode.txFEE_BUMP_INNER_SUCCESS | TransactionResultCode.txFEE_BUMP_INNER_FAILED =>
        TransactionNotAttempted(
          reason = xdr.getResult.getDiscriminant,
          feeCharged = fee,
          innerTransaction = Some(FeeBumpedTransactionResult.decodeXdr(xdr.getResult.getInnerResultPair))
        )

      case transactionResultCode => TransactionNotAttempted(
        reason = transactionResultCode,
        feeCharged = fee,
        innerTransaction = None
      )
    }
  }
}

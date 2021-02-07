package stellar.sdk.model.result

import okio.ByteString
import org.stellar.xdr.InnerTransactionResult.InnerTransactionResultResult
import org.stellar.xdr.TransactionResultCode.{txFAILED, txSUCCESS}
import org.stellar.xdr.{Hash, InnerTransactionResult, InnerTransactionResultPair, Int64, TransactionResultCode}
import stellar.sdk.model.NativeAmount

/** After attempting a fee bump, the results of the transaction to be bumped */
case class FeeBumpedTransactionResult(
  feeCharged: NativeAmount,
  result: TransactionResultCode,
  operationResults: List[OperationResult],
  hash: ByteString
) {

  def xdr: InnerTransactionResultPair = {
    new InnerTransactionResultPair.Builder()
      .result(new InnerTransactionResult.Builder()
        .feeCharged(new Int64(feeCharged.units))
        .result(new InnerTransactionResultResult.Builder()
          .discriminant(result)
          .results(if (result == txSUCCESS || result == txFAILED) operationResults.map(_.xdr).toArray else null)
          .build())
        .ext(new InnerTransactionResult.InnerTransactionResultExt.Builder()
          .discriminant(0)
          .build())
        .build())
      .transactionHash(new Hash(hash.toByteArray))
      .build()
  }

}

object FeeBumpedTransactionResult {

  def decodeXdr(xdr: InnerTransactionResultPair): FeeBumpedTransactionResult = {
    val result = xdr.getResult.getResult.getDiscriminant
    val operationResults = result match {
      case TransactionResultCode.txSUCCESS | TransactionResultCode.txFAILED =>
        xdr.getResult.getResult.getResults.map(OperationResult.decodeXdr).toList
      case _ => Nil
    }
    FeeBumpedTransactionResult(
      feeCharged = NativeAmount(xdr.getResult.getFeeCharged.getInt64),
      result = result,
      operationResults = operationResults,
      hash = new ByteString(xdr.getTransactionHash.getHash)
    )
  }

}
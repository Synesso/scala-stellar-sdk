package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{InflationResultCode, Int64, OperationType, InflationResult => XInflationResult}
import stellar.sdk.PublicKey
import stellar.sdk.model.NativeAmount

sealed abstract class InflationResult extends ProcessedOperationResult {
  val result: XInflationResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.INFLATION)
    .inflationResult(result)
    .build()
}

object InflationResult {
}

/**
  * Inflation operation was successful.
  */
case class InflationSuccess(payouts: Seq[InflationPayout]) extends InflationResult {
  val result: XInflationResult = new XInflationResult.Builder()
    .discriminant(InflationResultCode.INFLATION_SUCCESS)
    .payouts(payouts.map(_.xdr).toArray)
    .build()
}

/**
 * Inflation operation failed because inflation is not yet due.
 */
case object InflationNotDue extends InflationResult {
  val result: XInflationResult = new XInflationResult.Builder()
    .discriminant(InflationResultCode.INFLATION_NOT_TIME)
    .build()
}


case class InflationPayout(recipient: PublicKey, amount: NativeAmount) {
  def xdr: org.stellar.xdr.InflationPayout = new org.stellar.xdr.InflationPayout.Builder()
    .amount(new Int64(amount.units))
    .destination(recipient.toAccountId.accountIdXdr)
    .build()
}

object InflationPayout {
}
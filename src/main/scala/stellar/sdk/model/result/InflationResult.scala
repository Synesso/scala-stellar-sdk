package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{InflationResultCode, Int64, OperationType, InflationPayout => XInflationPayout, InflationResult => XInflationResult}
import stellar.sdk.PublicKey
import stellar.sdk.model.{AccountId, NativeAmount}

sealed abstract class InflationResult extends ProcessedOperationResult {
  def result: XInflationResult
  override def transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.INFLATION)
    .inflationResult(result)
    .build()
}

object InflationResult {
  def decodeXdr(xdr: XInflationResult): InflationResult = xdr.getDiscriminant match {
    case InflationResultCode.INFLATION_SUCCESS => InflationSuccess(
      payouts = xdr.getPayouts.map(InflationPayout.decodeXdr).toList
    )
    case InflationResultCode.INFLATION_NOT_TIME => InflationNotDue
  }
}

/**
 * Inflation operation was successful.
 */
case class InflationSuccess(payouts: Seq[InflationPayout]) extends InflationResult {
  override def result: XInflationResult = new XInflationResult.Builder()
    .discriminant(InflationResultCode.INFLATION_SUCCESS)
    .payouts(payouts.map(_.xdr).toArray)
    .build()
}

/**
 * Inflation operation failed because inflation is not yet due.
 */
case object InflationNotDue extends InflationResult {
  override def result: XInflationResult = new XInflationResult.Builder()
    .discriminant(InflationResultCode.INFLATION_NOT_TIME)
    .build()
}

case class InflationPayout(recipient: PublicKey, amount: NativeAmount) {
  def xdr: org.stellar.xdr.InflationPayout = new org.stellar.xdr.InflationPayout.Builder()
    .amount(new Int64(amount.units))
    .destination(recipient.toAccountId.xdr)
    .build()
}

object InflationPayout {
  def decodeXdr(xdr: XInflationPayout): InflationPayout = InflationPayout(
    recipient = AccountId.decodeXdr(xdr.getDestination).publicKey,
    amount = NativeAmount(xdr.getAmount.getInt64)
  )
}

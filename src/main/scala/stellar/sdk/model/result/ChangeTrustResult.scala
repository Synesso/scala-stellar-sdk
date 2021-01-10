package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{ChangeTrustResultCode, OperationType, ChangeTrustResult => XChangeTrustResult}

sealed abstract class ChangeTrustResult extends ProcessedOperationResult {
  val result: XChangeTrustResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.CHANGE_TRUST)
    .changeTrustResult(result)
    .build()
}

object ChangeTrustResult {
}

/**
 * ChangeTrust operation was successful.
 */
case object ChangeTrustSuccess extends ChangeTrustResult {
  val result: XChangeTrustResult = new XChangeTrustResult.Builder()
    .discriminant(ChangeTrustResultCode.CHANGE_TRUST_SUCCESS)
    .build()
}

/**
 * ChangeTrust operation failed because the request was malformed.
 * E.g. The limit was less than zero, or the asset was malformed, or the native asset was provided.
 */
case object ChangeTrustMalformed extends ChangeTrustResult {
  val result: XChangeTrustResult = new XChangeTrustResult.Builder()
    .discriminant(ChangeTrustResultCode.CHANGE_TRUST_MALFORMED)
    .build()
}

/**
 * ChangeTrust operation failed because the issuer account does not exist.
 */
case object ChangeTrustNoIssuer extends ChangeTrustResult {
  val result: XChangeTrustResult = new XChangeTrustResult.Builder()
    .discriminant(ChangeTrustResultCode.CHANGE_TRUST_NO_ISSUER)
    .build()
}

/**
 * ChangeTrust operation failed because the limit was zero or less than the current balance.
 */
case object ChangeTrustInvalidLimit extends ChangeTrustResult {
  val result: XChangeTrustResult = new XChangeTrustResult.Builder()
    .discriminant(ChangeTrustResultCode.CHANGE_TRUST_INVALID_LIMIT)
    .build()
}

/**
 * ChangeTrust operation failed because there is not enough funds in reserve to create a new trustline.
 */
case object ChangeTrustLowReserve extends ChangeTrustResult {
  val result: XChangeTrustResult = new XChangeTrustResult.Builder()
    .discriminant(ChangeTrustResultCode.CHANGE_TRUST_LOW_RESERVE)
    .build()
}

/**
 * ChangeTrust operation failed because it is not valid to trust your own issued asset.
 */
case object ChangeTrustSelfNotAllowed extends ChangeTrustResult {
  val result: XChangeTrustResult = new XChangeTrustResult.Builder()
    .discriminant(ChangeTrustResultCode.CHANGE_TRUST_SELF_NOT_ALLOWED)
    .build()
}
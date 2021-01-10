package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{CreateClaimableBalanceResultCode, OperationType, CreateClaimableBalanceResult => XCreateClaimableBalanceResult}
import stellar.sdk.model.ClaimableBalanceId

sealed abstract class CreateClaimableBalanceResult extends ProcessedOperationResult {
  val result: XCreateClaimableBalanceResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.CREATE_CLAIMABLE_BALANCE)
    .createClaimableBalanceResult(result)
    .build()
}

object CreateClaimableBalanceResult {
}

/**
 * CreateClaimableBalance operation was successful.
 *
 * @param id the identifier of the claimable balance created.
 */
case class CreateClaimableBalanceSuccess(id: ClaimableBalanceId) extends CreateClaimableBalanceResult {
  val result: XCreateClaimableBalanceResult = new XCreateClaimableBalanceResult.Builder()
    .discriminant(CreateClaimableBalanceResultCode.CREATE_CLAIMABLE_BALANCE_SUCCESS)
    .balanceID(id.xdr)
    .build()
}

/**
 * CreateClaimableBalance operation failed because the request was malformed
 */
case object CreateClaimableBalanceMalformed extends CreateClaimableBalanceResult {
  val result: XCreateClaimableBalanceResult = new XCreateClaimableBalanceResult.Builder()
    .discriminant(CreateClaimableBalanceResultCode.CREATE_CLAIMABLE_BALANCE_MALFORMED)
    .build()
}

/**
 * CreateClaimableBalance operation failed because the declared reserve was insufficient
 */
case object CreateClaimableBalanceLowReserve extends CreateClaimableBalanceResult {
  val result: XCreateClaimableBalanceResult = new XCreateClaimableBalanceResult.Builder()
    .discriminant(CreateClaimableBalanceResultCode.CREATE_CLAIMABLE_BALANCE_LOW_RESERVE)
    .build()
}

/**
 * CreateClaimableBalance operation failed because the required trustline does not exist
 */
case object CreateClaimableBalanceNoTrust extends CreateClaimableBalanceResult {
  val result: XCreateClaimableBalanceResult = new XCreateClaimableBalanceResult.Builder()
    .discriminant(CreateClaimableBalanceResultCode.CREATE_CLAIMABLE_BALANCE_NO_TRUST)
    .build()
}

/**
 * CreateClaimableBalance operation failed because the account was not authorized for this asset
 */
case object CreateClaimableBalanceNotAuthorized extends CreateClaimableBalanceResult {
  val result: XCreateClaimableBalanceResult = new XCreateClaimableBalanceResult.Builder()
    .discriminant(CreateClaimableBalanceResultCode.CREATE_CLAIMABLE_BALANCE_NOT_AUTHORIZED)
    .build()
}

/**
 * CreateClaimableBalance operation failed because the source account had insufficient funds
 */
case object CreateClaimableBalanceUnderfunded extends CreateClaimableBalanceResult {
  val result: XCreateClaimableBalanceResult = new XCreateClaimableBalanceResult.Builder()
    .discriminant(CreateClaimableBalanceResultCode.CREATE_CLAIMABLE_BALANCE_UNDERFUNDED)
    .build()
}
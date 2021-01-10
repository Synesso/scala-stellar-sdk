package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{AccountMergeResultCode, Int64, OperationType, AccountMergeResult => XAccountMergeResult}
import stellar.sdk.model.NativeAmount

sealed abstract class AccountMergeResult extends ProcessedOperationResult {
  val result: XAccountMergeResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.ACCOUNT_MERGE)
    .accountMergeResult(result)
    .build()
}

object AccountMergeResult {
}

/**
 * AccountMerge operation was successful.
 */
case class AccountMergeSuccess(sourceBalance: NativeAmount) extends AccountMergeResult {
  val result: XAccountMergeResult = new XAccountMergeResult.Builder()
    .discriminant(AccountMergeResultCode.ACCOUNT_MERGE_SUCCESS)
    .sourceAccountBalance(new Int64(sourceBalance.units))
    .build()
}

/**
 * AccountMerge operation failed because the request was malformed.
 * E.g. The source and destination accounts are the same.
 */
case object AccountMergeMalformed extends AccountMergeResult {
  val result: XAccountMergeResult = new XAccountMergeResult.Builder()
    .discriminant(AccountMergeResultCode.ACCOUNT_MERGE_MALFORMED)
    .build()
}

/**
 * AccountMerge operation failed because the destination account does not exist.
 */
case object AccountMergeNoAccount extends AccountMergeResult {
  val result: XAccountMergeResult = new XAccountMergeResult.Builder()
    .discriminant(AccountMergeResultCode.ACCOUNT_MERGE_NO_ACCOUNT)
    .build()
}

/**
 * AccountMerge operation failed because the source account has the AUTH_IMMUTABLE flag set.
 */
case object AccountMergeImmutable extends AccountMergeResult {
  val result: XAccountMergeResult = new XAccountMergeResult.Builder()
    .discriminant(AccountMergeResultCode.ACCOUNT_MERGE_IMMUTABLE_SET)
    .build()
}

/**
 * AccountMerge operation failed because the source account has trustlines and/or offers.
 */
case object AccountMergeHasSubEntries extends AccountMergeResult {
  val result: XAccountMergeResult = new XAccountMergeResult.Builder()
    .discriminant(AccountMergeResultCode.ACCOUNT_MERGE_HAS_SUB_ENTRIES)
    .build()
}

/**
 * AccountMerge operation failed because it would be possible to recreate it with an earlier sequence number.
 */
case object AccountMergeSeqNumTooFar extends AccountMergeResult {
  val result: XAccountMergeResult = new XAccountMergeResult.Builder()
    .discriminant(AccountMergeResultCode.ACCOUNT_MERGE_SEQNUM_TOO_FAR)
    .build()
}

/**
 * AccountMerge operation failed because the resulting destination account balance would be too large.
 */
case object AccountMergeDestinationFull extends AccountMergeResult {
  val result: XAccountMergeResult = new XAccountMergeResult.Builder()
    .discriminant(AccountMergeResultCode.ACCOUNT_MERGE_DEST_FULL)
    .build()
}

/**
 * AccountMerge operation failed because the account is sponsoring other accounts.
 */
case object AccountMergeIsSponsor extends AccountMergeResult {
  val result: XAccountMergeResult = new XAccountMergeResult.Builder()
    .discriminant(AccountMergeResultCode.ACCOUNT_MERGE_IS_SPONSOR)
    .build()
}

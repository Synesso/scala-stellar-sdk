package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{AllowTrustResultCode, OperationType, AllowTrustResult => XAllowTrustResult}

sealed abstract class AllowTrustResult extends ProcessedOperationResult {
  val result: XAllowTrustResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.ALLOW_TRUST)
    .allowTrustResult(result)
    .build()

}

object AllowTrustResult {
}

/**
 * AllowTrust operation was successful.
 */
case object AllowTrustSuccess extends AllowTrustResult {
  val result: XAllowTrustResult = new XAllowTrustResult.Builder()
    .discriminant(AllowTrustResultCode.ALLOW_TRUST_SUCCESS)
    .build()
}

/**
 * AllowTrust operation failed because the request was malformed.
 * E.g. The limit was less than zero, or the asset was malformed, or the native asset was provided.
 */
case object AllowTrustMalformed extends AllowTrustResult {
  val result: XAllowTrustResult = new XAllowTrustResult.Builder()
    .discriminant(AllowTrustResultCode.ALLOW_TRUST_MALFORMED)
    .build()
}

/**
 * AllowTrust operation failed because the trustor does not have a trustline.
 */
case object AllowTrustNoTrustLine extends AllowTrustResult {
  val result: XAllowTrustResult = new XAllowTrustResult.Builder()
    .discriminant(AllowTrustResultCode.ALLOW_TRUST_NO_TRUST_LINE)
    .build()
}

/**
 * AllowTrust operation failed because the source account does not require trust.
 */
case object AllowTrustNotRequired extends AllowTrustResult {
  val result: XAllowTrustResult = new XAllowTrustResult.Builder()
    .discriminant(AllowTrustResultCode.ALLOW_TRUST_TRUST_NOT_REQUIRED)
    .build()
}

/**
 * AllowTrust operation failed because the source account is unable to revoke trust.
 */
case object AllowTrustCannotRevoke extends AllowTrustResult {
  val result: XAllowTrustResult = new XAllowTrustResult.Builder()
    .discriminant(AllowTrustResultCode.ALLOW_TRUST_CANT_REVOKE)
    .build()
}

/**
 * AllowTrust operation failed because it is not valid to trust your own issued asset.
 */
case object AllowTrustSelfNotAllowed extends AllowTrustResult {
  val result: XAllowTrustResult = new XAllowTrustResult.Builder()
    .discriminant(AllowTrustResultCode.ALLOW_TRUST_SELF_NOT_ALLOWED)
    .build()
}

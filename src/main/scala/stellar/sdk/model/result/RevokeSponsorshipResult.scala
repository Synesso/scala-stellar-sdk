package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{OperationType, RevokeSponsorshipResultCode, RevokeSponsorshipResult => XRevokeSponsorshipResult}

sealed abstract class RevokeSponsorshipResult extends ProcessedOperationResult {
  def result: XRevokeSponsorshipResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.REVOKE_SPONSORSHIP)
    .revokeSponsorshipResult(result)
    .build()
}

object RevokeSponsorshipResult {
  def decodeXdr(xdr: XRevokeSponsorshipResult): RevokeSponsorshipResult = xdr.getDiscriminant match {
    case RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_SUCCESS => RevokeSponsorshipSuccess
    case RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_DOES_NOT_EXIST => RevokeSponsorshipDoesNotExist
    case RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_NOT_SPONSOR => RevokeSponsorshipNotSponsor
    case RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_LOW_RESERVE => RevokeSponsorshipLowReserve
    case RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_ONLY_TRANSFERABLE => RevokeSponsorshipOnlyTransferable
  }
}

/**
 * RevokeSponsorship operation was successful.
 */
case object RevokeSponsorshipSuccess extends RevokeSponsorshipResult {
  override def result: XRevokeSponsorshipResult = new XRevokeSponsorshipResult.Builder()
    .discriminant(RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_SUCCESS)
    .build()
}

/**
 * RevokeSponsorship operation failed because the sponsorship didn't exist.
 */
case object RevokeSponsorshipDoesNotExist extends RevokeSponsorshipResult {
  override def result: XRevokeSponsorshipResult = new XRevokeSponsorshipResult.Builder()
    .discriminant(RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_DOES_NOT_EXIST)
    .build()
}

/**
 * RevokeSponsorship operation failed because there was a sponsorship, but the sponsoring account did not authorise the
 * operation.
 */
case object RevokeSponsorshipNotSponsor extends RevokeSponsorshipResult {
  override def result: XRevokeSponsorshipResult = new XRevokeSponsorshipResult.Builder()
    .discriminant(RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_NOT_SPONSOR)
    .build()
}

/**
 * RevokeSponsorship operation failed because the account had insufficient reserves to cover the sponsored entries.
 */
case object RevokeSponsorshipLowReserve extends RevokeSponsorshipResult {
  override def result: XRevokeSponsorshipResult = new XRevokeSponsorshipResult.Builder()
    .discriminant(RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_LOW_RESERVE)
    .build()
}

/**
 * RevokeSponsorship operation failed because the sponsorship can only be transferred.
 */
case object RevokeSponsorshipOnlyTransferable extends RevokeSponsorshipResult {
  override def result: XRevokeSponsorshipResult = new XRevokeSponsorshipResult.Builder()
    .discriminant(RevokeSponsorshipResultCode.REVOKE_SPONSORSHIP_ONLY_TRANSFERABLE)
    .build()
}

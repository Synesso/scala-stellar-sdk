package stellar.sdk.model.result

import org.stellar.xdr.BeginSponsoringFutureReservesResultCode.{BEGIN_SPONSORING_FUTURE_RESERVES_ALREADY_SPONSORED, BEGIN_SPONSORING_FUTURE_RESERVES_MALFORMED, BEGIN_SPONSORING_FUTURE_RESERVES_RECURSIVE, BEGIN_SPONSORING_FUTURE_RESERVES_SUCCESS}
import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{BeginSponsoringFutureReservesResultCode, OperationType, BeginSponsoringFutureReservesResult => XBeginSponsoringFutureReservesResult}

sealed abstract class BeginSponsoringFutureReservesResult extends ProcessedOperationResult {
  def result: XBeginSponsoringFutureReservesResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.BEGIN_SPONSORING_FUTURE_RESERVES)
    .beginSponsoringFutureReservesResult(result)
    .build()

}

object BeginSponsoringFutureReservesResult {
  def decodeXdr(xdr: XBeginSponsoringFutureReservesResult): BeginSponsoringFutureReservesResult = xdr.getDiscriminant match {
    case BEGIN_SPONSORING_FUTURE_RESERVES_SUCCESS => BeginSponsoringFutureReservesSuccess
    case BEGIN_SPONSORING_FUTURE_RESERVES_ALREADY_SPONSORED => BeginSponsoringFutureReservesAlreadySponsored
    case BEGIN_SPONSORING_FUTURE_RESERVES_MALFORMED => BeginSponsoringFutureReservesMalformed
    case BEGIN_SPONSORING_FUTURE_RESERVES_RECURSIVE => BeginSponsoringFutureReservesRecursive
  }
}

/**
 * BeginSponsoringFutureReserves operation was successful.
 */
case object BeginSponsoringFutureReservesSuccess extends BeginSponsoringFutureReservesResult {
  override def result: XBeginSponsoringFutureReservesResult = new XBeginSponsoringFutureReservesResult.Builder()
    .discriminant(BeginSponsoringFutureReservesResultCode.BEGIN_SPONSORING_FUTURE_RESERVES_SUCCESS)
    .build()
}

/**
 * BeginSponsoringFutureReserves operation failed because there was insufficient reserve funds to add another signer.
 */
case object BeginSponsoringFutureReservesAlreadySponsored extends BeginSponsoringFutureReservesResult {
  override def result: XBeginSponsoringFutureReservesResult = new XBeginSponsoringFutureReservesResult.Builder()
    .discriminant(BeginSponsoringFutureReservesResultCode.BEGIN_SPONSORING_FUTURE_RESERVES_ALREADY_SPONSORED)
    .build()
}

/**
 * BeginSponsoringFutureReserves operation failed because the maximum number of signers has already been met.
 */
case object BeginSponsoringFutureReservesMalformed extends BeginSponsoringFutureReservesResult {
  override def result: XBeginSponsoringFutureReservesResult = new XBeginSponsoringFutureReservesResult.Builder()
    .discriminant(BeginSponsoringFutureReservesResultCode.BEGIN_SPONSORING_FUTURE_RESERVES_MALFORMED)
    .build()
}

/**
 * BeginSponsoringFutureReserves operation failed because there was an invalid combination of set/clear flags.
 */
case object BeginSponsoringFutureReservesRecursive extends BeginSponsoringFutureReservesResult {
  override def result: XBeginSponsoringFutureReservesResult = new XBeginSponsoringFutureReservesResult.Builder()
    .discriminant(BeginSponsoringFutureReservesResultCode.BEGIN_SPONSORING_FUTURE_RESERVES_RECURSIVE)
    .build()
}

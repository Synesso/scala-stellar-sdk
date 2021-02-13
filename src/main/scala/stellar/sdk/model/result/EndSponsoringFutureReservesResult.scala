package stellar.sdk.model.result

import org.stellar.xdr.EndSponsoringFutureReservesResultCode.{END_SPONSORING_FUTURE_RESERVES_NOT_SPONSORED, END_SPONSORING_FUTURE_RESERVES_SUCCESS}
import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{EndSponsoringFutureReservesResultCode, OperationType, EndSponsoringFutureReservesResult => XEndSponsoringFutureReservesResult}

sealed abstract class EndSponsoringFutureReservesResult extends ProcessedOperationResult {
  def result: XEndSponsoringFutureReservesResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.END_SPONSORING_FUTURE_RESERVES)
    .endSponsoringFutureReservesResult(result)
    .build()

}

object EndSponsoringFutureReservesResult {
  def decodeXdr(xdr: XEndSponsoringFutureReservesResult): EndSponsoringFutureReservesResult = xdr.getDiscriminant match {
    case END_SPONSORING_FUTURE_RESERVES_SUCCESS => EndSponsoringFutureReservesSuccess
    case END_SPONSORING_FUTURE_RESERVES_NOT_SPONSORED => EndSponsoringFutureReservesNotSponsored
  }
}

/**
 * EndSponsoringFutureReserves operation was successful.
 */
case object EndSponsoringFutureReservesSuccess extends EndSponsoringFutureReservesResult {
  override def result: XEndSponsoringFutureReservesResult = new XEndSponsoringFutureReservesResult.Builder()
    .discriminant(EndSponsoringFutureReservesResultCode.END_SPONSORING_FUTURE_RESERVES_SUCCESS)
    .build()
}

/**
 * EndSponsoringFutureReserves operation failed because there was insufficient reserve funds to add another signer.
 */
case object EndSponsoringFutureReservesNotSponsored extends EndSponsoringFutureReservesResult {
  override def result: XEndSponsoringFutureReservesResult = new XEndSponsoringFutureReservesResult.Builder()
    .discriminant(EndSponsoringFutureReservesResultCode.END_SPONSORING_FUTURE_RESERVES_NOT_SPONSORED)
    .build()
}

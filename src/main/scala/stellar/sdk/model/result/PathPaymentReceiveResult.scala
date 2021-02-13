package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.PathPaymentStrictReceiveResult.PathPaymentStrictReceiveResultSuccess
import org.stellar.xdr.{Int64, OperationType, PathPaymentStrictReceiveResultCode, SimplePaymentResult, PathPaymentStrictReceiveResult => XReceiveResult}
import stellar.sdk.PublicKey
import stellar.sdk.model.{AccountId, Amount, Asset}

sealed abstract class PathPaymentReceiveResult extends ProcessedOperationResult {
  def result: XReceiveResult
  override def transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.PATH_PAYMENT_STRICT_RECEIVE)
    .pathPaymentStrictReceiveResult(result)
    .build()
}

object PathPaymentReceiveResult  {
  def decodeXdr(xdr: XReceiveResult): PathPaymentReceiveResult = xdr.getDiscriminant match {
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_SUCCESS => PathPaymentReceiveSuccess(
      claims = xdr.getSuccess.getOffers.map(OfferClaim.decodeXdr).toList,
      destination = AccountId.decodeXdr(xdr.getSuccess.getLast.getDestination).publicKey,
      paid = Amount(
        units = xdr.getSuccess.getLast.getAmount.getInt64,
        asset = Asset.decodeXdr(xdr.getSuccess.getLast.getAsset)
      )
    )
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_MALFORMED => PathPaymentReceiveMalformed
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_UNDERFUNDED => PathPaymentReceiveUnderfunded
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_SRC_NO_TRUST => PathPaymentReceiveSourceNoTrust
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_SRC_NOT_AUTHORIZED => PathPaymentReceiveSourceNotAuthorised
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_NO_DESTINATION => PathPaymentReceiveNoDestination
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_NO_TRUST => PathPaymentReceiveDestinationNoTrust
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_NOT_AUTHORIZED => PathPaymentReceiveDestinationNotAuthorised
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_LINE_FULL => PathPaymentReceiveDestinationLineFull
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_NO_ISSUER => PathPaymentReceiveNoIssuer(Asset.decodeXdr(xdr.getNoIssuer))
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_TOO_FEW_OFFERS => PathPaymentReceiveTooFewOffers
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_OFFER_CROSS_SELF => PathPaymentReceiveOfferCrossesSelf
    case PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_OVER_SENDMAX => PathPaymentReceiveSendMaxExceeded

  }
}

/**
 * PathPayment operation was successful.
 * @param claims the trades that were effected during this path payment.
 */
case class PathPaymentReceiveSuccess(
  claims: Seq[OfferClaim],
  destination: PublicKey,
  paid: Amount
) extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_SUCCESS)
    .success(new PathPaymentStrictReceiveResultSuccess.Builder()
      .last(new SimplePaymentResult.Builder()
        .amount(new Int64(paid.units))
        .asset(paid.asset.xdr)
        .destination(destination.toAccountId.xdr)
        .build())
      .offers(claims.map(_.xdr).toArray)
      .build())
    .build()
}

/**
 * PathPayment operation failed because the request was malformed.
 * E.g. The destination or sendMax amounts were negative, or the any of the asset were invalid.
 */
case object PathPaymentReceiveMalformed extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_MALFORMED)
    .build()
}

/**
 * PathPayment operation failed because there were insufficient funds in the source account.
 */
case object PathPaymentReceiveUnderfunded extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_UNDERFUNDED)
    .build()
}

/**
 * PathPayment operation failed because the sender has not trustline for the specified asset.
 * (Additionally, this implies the sender doesn't have the funds to send anyway).
 */
case object PathPaymentReceiveSourceNoTrust extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_SRC_NO_TRUST)
    .build()
}

/**
 * PathPayment operation failed because the sender is not authorised to send the specified asset.
 */
case object PathPaymentReceiveSourceNotAuthorised extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_SRC_NOT_AUTHORIZED)
    .build()
}

/**
 * PathPayment operation failed because the destination account did not exist.
 */
case object PathPaymentReceiveNoDestination extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_NO_DESTINATION)
    .build()
}

/**
 * PathPayment operation failed because the destination account does not have a trustline for the asset.
 */
case object PathPaymentReceiveDestinationNoTrust extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_NO_TRUST)
    .build()
}

/**
 * PathPayment operation failed because the destination account is not authorised to hold the asset.
 */
case object PathPaymentReceiveDestinationNotAuthorised extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_NOT_AUTHORIZED)
    .build()
}

/**
 * PathPayment operation failed because it would have put the destination account's balance over the limit for the asset.
 */
case object PathPaymentReceiveDestinationLineFull extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_LINE_FULL)
    .build()
}

/**
 * PathPayment operation failed because there was no issuer for one or more of the assets.
 * @param asset the asset for which there's no issuer.
 */
case class PathPaymentReceiveNoIssuer(asset: Asset) extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_NO_ISSUER)
    .noIssuer(asset.xdr)
    .build()
}

/**
 * PathPayment operation failed because there were too few offers to satisfy the path.
 */
case object PathPaymentReceiveTooFewOffers extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_TOO_FEW_OFFERS)
    .build()
}

/**
 * PathPayment operation failed because it would have resulted in matching its own offer.
 */
case object PathPaymentReceiveOfferCrossesSelf extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_OFFER_CROSS_SELF)
    .build()
}

/**
 * PathPayment operation failed because it could not be effected without sending more than the specified maximum.
 */
case object PathPaymentReceiveSendMaxExceeded extends PathPaymentReceiveResult {
  override def result: XReceiveResult = new XReceiveResult.Builder()
    .discriminant(PathPaymentStrictReceiveResultCode.PATH_PAYMENT_STRICT_RECEIVE_OVER_SENDMAX)
    .build()
}
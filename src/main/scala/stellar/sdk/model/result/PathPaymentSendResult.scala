package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.PathPaymentStrictSendResult.PathPaymentStrictSendResultSuccess
import org.stellar.xdr.{Int64, OperationType, PathPaymentStrictSendResult => XSendResult, PathPaymentStrictSendResultCode, SimplePaymentResult}
import stellar.sdk.PublicKey
import stellar.sdk.model.{AccountId, Amount, Asset}

sealed abstract class PathPaymentSendResult extends ProcessedOperationResult {
  def result: XSendResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.PATH_PAYMENT_STRICT_SEND)
    .pathPaymentStrictSendResult(result)
    .build()
}

object PathPaymentSendResult {
  def decodeXdr(xdr: XSendResult): PathPaymentSendResult = xdr.getDiscriminant match {
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_SUCCESS => PathPaymentSendSuccess(
      claims = xdr.getSuccess.getOffers.map(OfferClaim.decodeXdr).toList,
      destination = AccountId.decodeXdr(xdr.getSuccess.getLast.getDestination).publicKey,
      paid = Amount(
        units = xdr.getSuccess.getLast.getAmount.getInt64,
        asset = Asset.decodeXdr(xdr.getSuccess.getLast.getAsset)
      )
    )
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_MALFORMED => PathPaymentSendMalformed
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_UNDERFUNDED => PathPaymentSendUnderfunded
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_SRC_NO_TRUST => PathPaymentSendSourceNoTrust
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_SRC_NOT_AUTHORIZED => PathPaymentSendSourceNotAuthorised
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_NO_DESTINATION => PathPaymentSendNoDestination
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_NO_TRUST => PathPaymentSendDestinationNoTrust
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_NOT_AUTHORIZED => PathPaymentSendDestinationNotAuthorised
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_LINE_FULL => PathPaymentSendDestinationLineFull
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_NO_ISSUER => PathPaymentSendNoIssuer(
      asset = Asset.decodeXdr(xdr.getNoIssuer)
    )
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_TOO_FEW_OFFERS => PathPaymentSendTooFewOffers
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_OFFER_CROSS_SELF => PathPaymentSendOfferCrossesSelf
    case PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_UNDER_DESTMIN => PathPaymentSendDestMinNotMet
  }
}

/**
 * PathPayment operation was successful.
 * @param claims the trades that were effected during this path payment.
 */
case class PathPaymentSendSuccess(
  claims: Seq[OfferClaim],
  destination: PublicKey,
  paid: Amount
) extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_SUCCESS)
    .success(new PathPaymentStrictSendResultSuccess.Builder()
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
case object PathPaymentSendMalformed extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_MALFORMED)
    .build()
}

/**
 * PathPayment operation failed because there were insufficient funds in the source account.
 */
case object PathPaymentSendUnderfunded extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_UNDERFUNDED)
    .build()
}

/**
 * PathPayment operation failed because the sender has not trustline for the specified asset.
 * (Additionally, this implies the sender doesn't have the funds to send anyway).
 */
case object PathPaymentSendSourceNoTrust extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_SRC_NO_TRUST)
    .build()
}

/**
 * PathPayment operation failed because the sender is not authorised to send the specified asset.
 */
case object PathPaymentSendSourceNotAuthorised extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_SRC_NOT_AUTHORIZED)
    .build()
}

/**
 * PathPayment operation failed because the destination account did not exist.
 */
case object PathPaymentSendNoDestination extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_NO_DESTINATION)
    .build()
}

/**
 * PathPayment operation failed because the destination account does not have a trustline for the asset.
 */
case object PathPaymentSendDestinationNoTrust extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_NO_TRUST)
    .build()
}

/**
 * PathPayment operation failed because the destination account is not authorised to hold the asset.
 */
case object PathPaymentSendDestinationNotAuthorised extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_NOT_AUTHORIZED)
    .build()
}

/**
 * PathPayment operation failed because it would have put the destination account's balance over the limit for the asset.
 */
case object PathPaymentSendDestinationLineFull extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_LINE_FULL)
    .build()
}

/**
 * PathPayment operation failed because there was no issuer for one or more of the assets.
 * @param asset the asset for which there's no issuer.
 */
case class PathPaymentSendNoIssuer(asset: Asset) extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_NO_ISSUER)
    .noIssuer(asset.xdr)
    .build()
}

/**
 * PathPayment operation failed because there were too few offers to satisfy the path.
 */
case object PathPaymentSendTooFewOffers extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_TOO_FEW_OFFERS)
    .build()
}

/**
 * PathPayment operation failed because it would have resulted in matching its own offer.
 */
case object PathPaymentSendOfferCrossesSelf extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_OFFER_CROSS_SELF)
    .build()
}

/**
 * PathPayment operation failed because it could not be effected without sending less than the specified minimum.
 */
case object PathPaymentSendDestMinNotMet extends PathPaymentSendResult {
  override def result: XSendResult = new XSendResult.Builder()
    .discriminant(PathPaymentStrictSendResultCode.PATH_PAYMENT_STRICT_SEND_UNDER_DESTMIN)
    .build()
}
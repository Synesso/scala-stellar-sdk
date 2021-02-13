package stellar.sdk.model.result
import org.stellar.xdr.ManageOfferSuccessResult.ManageOfferSuccessResultOffer
import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{ManageOfferEffect, ManageOfferSuccessResult, ManageSellOfferResult, ManageSellOfferResultCode, OperationType}
import stellar.sdk.model.ledger.OfferEntry

sealed abstract class CreatePassiveSellOfferResult extends ProcessedOperationResult {
  def result: ManageSellOfferResult
  override def transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.CREATE_PASSIVE_SELL_OFFER)
    .createPassiveSellOfferResult(result)
    .build()
}

object CreatePassiveSellOfferResult {
  def decodeXdr(xdr: ManageSellOfferResult): CreatePassiveSellOfferResult = xdr.getDiscriminant match {
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_SUCCESS => CreatePassiveSellOfferSuccess(
      OfferEntry.decodeXdr(xdr.getSuccess.getOffer.getOffer),
      xdr.getSuccess.getOffersClaimed.map(OfferClaim.decodeXdr).toList
    )
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_MALFORMED => CreatePassiveSellOfferMalformed
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_TRUST => CreatePassiveSellOfferSellNoTrust
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_TRUST => CreatePassiveSellOfferBuyNoTrust
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NOT_AUTHORIZED => CreatePassiveSellOfferSellNoAuth
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NOT_AUTHORIZED => CreatePassiveSellOfferBuyNoAuth
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_LINE_FULL => CreatePassiveSellOfferLineFull
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_UNDERFUNDED => CreatePassiveSellOfferUnderfunded
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_CROSS_SELF => CreatePassiveSellOfferCrossSelf
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_ISSUER => CreatePassiveSellOfferSellNoIssuer
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_ISSUER => CreatePassiveSellOfferBuyNoIssuer
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_NOT_FOUND => UpdatePassiveOfferIdNotFound
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_LOW_RESERVE => CreatePassiveSellOfferLowReserve
  }
}

/**
 * CreatePassiveSellOffer operation was successful.
 */
case class CreatePassiveSellOfferSuccess(
  offer: OfferEntry,
  offerClaims: List[OfferClaim]
) extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SUCCESS)
    .success(new ManageOfferSuccessResult.Builder()
      .offer(new ManageOfferSuccessResultOffer.Builder()
        .discriminant(ManageOfferEffect.MANAGE_OFFER_CREATED)
        .offer(offer.xdr.getOffer)
        .build())
      .offersClaimed(offerClaims.map(_.xdr).toArray)
      .build())
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because the request was malformed.
 * E.g. Either of the assets were invalid, the assets were the same as each other,
 * the amount was less than zero, or the price numerator or denominator were zero or less.
 */
case object CreatePassiveSellOfferMalformed extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_MALFORMED)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because there was no trustline for what was being offered.
 * (This also implies the account was underfunded).
 */
case object CreatePassiveSellOfferSellNoTrust extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_TRUST)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because there was no trustline for what was being sought.
 */
case object CreatePassiveSellOfferBuyNoTrust extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_TRUST)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because the account is not authorised to sell the offered asset.
 */
case object CreatePassiveSellOfferSellNoAuth extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NOT_AUTHORIZED)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because the account is not authorised to buy the sought asset.
 */
case object CreatePassiveSellOfferBuyNoAuth extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NOT_AUTHORIZED)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because it would have put the account's balance over the limit for the sought asset.
 */
case object CreatePassiveSellOfferLineFull extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_LINE_FULL)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because there was an insufficient balance of the asset being offered to meet the offer.
 */
case object CreatePassiveSellOfferUnderfunded extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_UNDERFUNDED)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because it would have matched with an offer from the same account.
 */
case object CreatePassiveSellOfferCrossSelf extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_CROSS_SELF)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because there is no issuer for the asset being offered.
 */
case object CreatePassiveSellOfferSellNoIssuer extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_ISSUER)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because there is no issuer for the asset being sought.
 */
case object CreatePassiveSellOfferBuyNoIssuer extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_ISSUER)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because it was an update attempt, but an offer with the given id did not exist.
 */
case object UpdatePassiveOfferIdNotFound extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_NOT_FOUND)
    .build()
}

/**
 * CreatePassiveSellOffer operation failed because the cumulative amount of it & all current offers from the same account
 * would exceed the account's available balance.
 */
case object CreatePassiveSellOfferLowReserve extends CreatePassiveSellOfferResult {
  override def result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_LOW_RESERVE)
    .build()
}
package stellar.sdk.model.result

import org.stellar.xdr.ManageOfferSuccessResult.ManageOfferSuccessResultOffer
import org.stellar.xdr.OperationResult.OperationResultTr
import stellar.sdk.model.ledger.OfferEntry
import org.stellar.xdr.{ManageOfferEffect, ManageOfferSuccessResult, ManageSellOfferResultCode, OperationType, ManageSellOfferResult => XManageSellOfferResult}

sealed abstract class ManageSellOfferResult extends ProcessedOperationResult {
  def result: XManageSellOfferResult
  override def transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.MANAGE_SELL_OFFER)
    .manageSellOfferResult(result)
    .build()
}

object ManageSellOfferResult {
  def decodeXdr(xdr: XManageSellOfferResult): ManageSellOfferResult = xdr.getDiscriminant match {
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_SUCCESS => ManageSellOfferSuccess(
      claims = xdr.getSuccess.getOffersClaimed.map(OfferClaim.decodeXdr).toList,
      entry = Option(xdr.getSuccess.getOffer.getOffer).map(OfferEntry.decodeXdr),
      effect = xdr.getSuccess.getOffer.getDiscriminant
    )
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_MALFORMED => ManageSellOfferMalformed
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_TRUST => ManageSellOfferSellNoTrust
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_TRUST => ManageSellOfferBuyNoTrust
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NOT_AUTHORIZED => ManageSellOfferSellNoAuth
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NOT_AUTHORIZED => ManageSellOfferBuyNoAuth
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_LINE_FULL => ManageSellOfferLineFull
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_UNDERFUNDED => ManageSellOfferUnderfunded
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_CROSS_SELF => ManageSellOfferCrossSelf
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_ISSUER => ManageSellOfferSellNoIssuer
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_ISSUER => ManageSellOfferBuyNoIssuer
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_NOT_FOUND => UpdateSellOfferIdNotFound
    case ManageSellOfferResultCode.MANAGE_SELL_OFFER_LOW_RESERVE => ManageSellOfferLowReserve
  }
}

/**
 * ManageOffer operation was successful.
 *
 * @param claims the trades that were effected as a result of posting this offer.
 * @param entry the offer entry that was newly created or updated.
 */
case class ManageSellOfferSuccess(
  claims: List[OfferClaim],
  entry: Option[OfferEntry],
  effect: ManageOfferEffect
) extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SUCCESS)
    .success(new ManageOfferSuccessResult.Builder()
      .offer(new ManageOfferSuccessResultOffer.Builder()
        .discriminant(effect)
        .offer(entry.map(_.xdr.getOffer).orNull)
        .build())
      .offersClaimed(claims.map(_.xdr).toArray)
      .build())
    .build()
}

/**
 * ManageOffer operation failed because the request was malformed.
 * E.g. Either of the assets were invalid, the assets were the same as each other,
 * the amount was less than zero, or the price numerator or denominator were zero or less.
 */
case object ManageSellOfferMalformed extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_MALFORMED)
    .build()
}

/**
 * ManageOffer operation failed because there was no trustline for what was being offered.
 * (This also implies the account was underfunded).
 */
case object ManageSellOfferSellNoTrust extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_TRUST)
    .build()
}

/**
 * ManageOffer operation failed because there was no trustline for what was being sought.
 */
case object ManageSellOfferBuyNoTrust extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_TRUST)
    .build()
}

/**
 * ManageOffer operation failed because the account is not authorised to sell the offered asset.
 */
case object ManageSellOfferSellNoAuth extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NOT_AUTHORIZED)
    .build()
}

/**
 * ManageOffer operation failed because the account is not authorised to buy the sought asset.
 */
case object ManageSellOfferBuyNoAuth extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NOT_AUTHORIZED)
    .build()
}

/**
 * ManageOffer operation failed because it would have put the account's balance over the limit for the sought asset.
 */
case object ManageSellOfferLineFull extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_LINE_FULL)
    .build()
}

/**
 * ManageOffer operation failed because there was an insufficient balance of the asset being offered to meet the offer.
 */
case object ManageSellOfferUnderfunded extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_UNDERFUNDED)
    .build()
}

/**
 * ManageOffer operation failed because it would have matched with an offer from the same account.
 */
case object ManageSellOfferCrossSelf extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_CROSS_SELF)
    .build()
}

/**
 * ManageOffer operation failed because there is no issuer for the asset being offered.
 */
case object ManageSellOfferSellNoIssuer extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_ISSUER)
    .build()
}

/**
 * ManageOffer operation failed because there is no issuer for the asset being sought.
 */
case object ManageSellOfferBuyNoIssuer extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_ISSUER)
    .build()
}

/**
 * ManageOffer operation failed because it was an update attempt, but an offer with the given id did not exist.
 */
case object UpdateSellOfferIdNotFound extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_NOT_FOUND)
    .build()
}

/**
 * ManageOffer operation failed because the cumulative amount of it & all current offers from the same account
 * would exceed the account's available balance.
 */
case object ManageSellOfferLowReserve extends ManageSellOfferResult {
  override def result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_LOW_RESERVE)
    .build()
}
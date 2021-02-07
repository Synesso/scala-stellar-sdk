package stellar.sdk.model.result

import org.stellar.xdr.ManageOfferSuccessResult.ManageOfferSuccessResultOffer
import org.stellar.xdr.OperationResult.OperationResultTr
import stellar.sdk.model.ledger.OfferEntry
import org.stellar.xdr.{ManageOfferEffect, ManageOfferSuccessResult, ManageBuyOfferResultCode, OperationType, ManageBuyOfferResult => XManageBuyOfferResult}

sealed abstract class ManageBuyOfferResult extends ProcessedOperationResult {
  def result: XManageBuyOfferResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.MANAGE_BUY_OFFER)
    .manageBuyOfferResult(result)
    .build()
}

object ManageBuyOfferResult {
  def decodeXdr(xdr: XManageBuyOfferResult): ManageBuyOfferResult = xdr.getDiscriminant match {
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_SUCCESS => ManageBuyOfferSuccess(
      claims = xdr.getSuccess.getOffersClaimed.map(OfferClaim.decodeXdr).toList,
      entry = Option(xdr.getSuccess.getOffer.getOffer).map(OfferEntry.decodeXdr),
      effect = xdr.getSuccess.getOffer.getDiscriminant
    )
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_MALFORMED => ManageBuyOfferMalformed
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_SELL_NO_TRUST => ManageBuyOfferSellNoTrust
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_BUY_NO_TRUST => ManageBuyOfferBuyNoTrust
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_SELL_NOT_AUTHORIZED => ManageBuyOfferSellNoAuth
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_BUY_NOT_AUTHORIZED => ManageBuyOfferBuyNoAuth
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_LINE_FULL => ManageBuyOfferLineFull
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_UNDERFUNDED => ManageBuyOfferUnderfunded
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_CROSS_SELF => ManageBuyOfferCrossSelf
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_SELL_NO_ISSUER => ManageBuyOfferSellNoIssuer
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_BUY_NO_ISSUER => ManageBuyOfferBuyNoIssuer
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_NOT_FOUND => UpdateBuyOfferIdNotFound
    case ManageBuyOfferResultCode.MANAGE_BUY_OFFER_LOW_RESERVE => ManageBuyOfferLowReserve
  }
}

/**
 * ManageOffer operation was successful.
 *
 * @param claims the trades that were effected as a result of posting this offer.
 * @param entry the offer entry that was newly created or updated.
 */
case class ManageBuyOfferSuccess(
  claims: Seq[OfferClaim],
  entry: Option[OfferEntry],
  effect: ManageOfferEffect
) extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_SUCCESS)
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
case object ManageBuyOfferMalformed extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_MALFORMED)
    .build()
}

/**
 * ManageOffer operation failed because there was no trustline for what was being offered.
 * (This also implies the account was underfunded).
 */
case object ManageBuyOfferSellNoTrust extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_SELL_NO_TRUST)
    .build()
}

/**
 * ManageOffer operation failed because there was no trustline for what was being sought.
 */
case object ManageBuyOfferBuyNoTrust extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_BUY_NO_TRUST)
    .build()
}

/**
 * ManageOffer operation failed because the account is not authorised to sell the offered asset.
 */
case object ManageBuyOfferSellNoAuth extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_SELL_NOT_AUTHORIZED)
    .build()
}

/**
 * ManageOffer operation failed because the account is not authorised to buy the sought asset.
 */
case object ManageBuyOfferBuyNoAuth extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_BUY_NOT_AUTHORIZED)
    .build()
}

/**
 * ManageOffer operation failed because it would have put the account's balance over the limit for the sought asset.
 */
case object ManageBuyOfferLineFull extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_LINE_FULL)
    .build()
}

/**
 * ManageOffer operation failed because there was an insufficient balance of the asset being offered to meet the offer.
 */
case object ManageBuyOfferUnderfunded extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_UNDERFUNDED)
    .build()
}

/**
 * ManageOffer operation failed because it would have matched with an offer from the same account.
 */
case object ManageBuyOfferCrossSelf extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_CROSS_SELF)
    .build()
}

/**
 * ManageOffer operation failed because there is no issuer for the asset being offered.
 */
case object ManageBuyOfferSellNoIssuer extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_SELL_NO_ISSUER)
    .build()
}

/**
 * ManageOffer operation failed because there is no issuer for the asset being sought.
 */
case object ManageBuyOfferBuyNoIssuer extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_BUY_NO_ISSUER)
    .build()
}

/**
 * ManageOffer operation failed because it was an update attempt, but an offer with the given id did not exist.
 */
case object UpdateBuyOfferIdNotFound extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_NOT_FOUND)
    .build()
}

/**
 * ManageOffer operation failed because the cumulative amount of it & all current offers from the same account
 * would exceed the account's available balance.
 */
case object ManageBuyOfferLowReserve extends ManageBuyOfferResult {
  override def result: XManageBuyOfferResult = new XManageBuyOfferResult.Builder()
    .discriminant(ManageBuyOfferResultCode.MANAGE_BUY_OFFER_LOW_RESERVE)
    .build()
}
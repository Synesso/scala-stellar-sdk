package stellar.sdk.model.result

import org.stellar.xdr.ManageOfferSuccessResult.ManageOfferSuccessResultOffer
import org.stellar.xdr.OperationResult.OperationResultTr
import stellar.sdk.model.ledger.OfferEntry
import org.stellar.xdr.{ManageOfferEffect, ManageOfferSuccessResult, ManageSellOfferResultCode, OperationType, ManageSellOfferResult => XManageSellOfferResult}

// TODO rename to ManageSellOfferResult ?
sealed abstract class ManageOfferResult extends ProcessedOperationResult {
  val result: XManageSellOfferResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.MANAGE_SELL_OFFER)
    .manageSellOfferResult(result)
    .build()
}

object ManageOfferResult {
}

/**
  * ManageOffer operation was successful.
  *
  * @param claims the trades that were effected as a result of posting this offer.
  * @param entry the offer entry that was newly created or updated.
  */
case class ManageOfferSuccess(
  claims: Seq[OfferClaim],
  entry: Option[OfferEntry],
  effect: ManageOfferEffect
) extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SUCCESS)
    .success(new ManageOfferSuccessResult.Builder()
      .offer(new ManageOfferSuccessResultOffer.Builder()
        .discriminant(effect)
        .build())
      .build())
    .build()
}

/**
  * ManageOffer operation failed because the request was malformed.
  * E.g. Either of the assets were invalid, the assets were the same as each other,
  * the amount was less than zero, or the price numerator or denominator were zero or less.
  */
case object ManageOfferMalformed extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_MALFORMED)
    .build()
}

/**
  * ManageOffer operation failed because there was no trustline for what was being offered.
  * (This also implies the account was underfunded).
  */
case object ManageOfferSellNoTrust extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_TRUST)
    .build()
}

/**
  * ManageOffer operation failed because there was no trustline for what was being sought.
  */
case object ManageOfferBuyNoTrust extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_TRUST)
    .build()
}

/**
  * ManageOffer operation failed because the account is not authorised to sell the offered asset.
  */
case object ManageOfferSellNoAuth extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NOT_AUTHORIZED)
    .build()
}

/**
  * ManageOffer operation failed because the account is not authorised to buy the sought asset.
  */
case object ManageOfferBuyNoAuth extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NOT_AUTHORIZED)
    .build()
}

/**
  * ManageOffer operation failed because it would have put the account's balance over the limit for the sought asset.
  */
case object ManageOfferLineFull extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_LINE_FULL)
    .build()
}

/**
  * ManageOffer operation failed because there was an insufficient balance of the asset being offered to meet the offer.
  */
case object ManageOfferUnderfunded extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_UNDERFUNDED)
    .build()
}

/**
  * ManageOffer operation failed because it would have matched with an offer from the same account.
  */
case object ManageOfferCrossSelf extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_CROSS_SELF)
    .build()
}

/**
  * ManageOffer operation failed because there is no issuer for the asset being offered.
  */
case object ManageOfferSellNoIssuer extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_ISSUER)
    .build()
}

/**
  * ManageOffer operation failed because there is no issuer for the asset being sought.
  */
case object ManageOfferBuyNoIssuer extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_ISSUER)
    .build()
}

/**
  * ManageOffer operation failed because it was an update attempt, but an offer with the given id did not exist.
  */
case object UpdateOfferIdNotFound extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_NOT_FOUND)
    .build()
}

/**
  * ManageOffer operation failed because the cumulative amount of it & all current offers from the same account
  * would exceed the account's available balance.
  */
case object ManageOfferLowReserve extends ManageOfferResult {
  val result: XManageSellOfferResult = new XManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_LOW_RESERVE)
    .build()
}



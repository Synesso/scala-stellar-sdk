package stellar.sdk.model.result
import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{ManageSellOfferResult, ManageSellOfferResultCode, OperationType}

sealed abstract class CreatePassiveSellOfferResult extends ProcessedOperationResult {
  val result: ManageSellOfferResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.ACCOUNT_MERGE)
    .createPassiveSellOfferResult(result)
    .build()
}

object CreatePassiveSellOfferResult {
}

/**
  * CreatePassiveSellOffer operation was successful.
  */
case object CreatePassiveSellOfferSuccess extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SUCCESS)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because the request was malformed.
  * E.g. Either of the assets were invalid, the assets were the same as each other,
  * the amount was less than zero, or the price numerator or denominator were zero or less.
  */
case object CreatePassiveSellOfferMalformed extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_MALFORMED)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because there was no trustline for what was being offered.
  * (This also implies the account was underfunded).
  */
case object CreatePassiveSellOfferSellNoTrust extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_TRUST)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because there was no trustline for what was being sought.
  */
case object CreatePassiveSellOfferBuyNoTrust extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_TRUST)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because the account is not authorised to sell the offered asset.
  */
case object CreatePassiveSellOfferSellNoAuth extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NOT_AUTHORIZED)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because the account is not authorised to buy the sought asset.
  */
case object CreatePassiveSellOfferBuyNoAuth extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NOT_AUTHORIZED)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because it would have put the account's balance over the limit for the sought asset.
  */
case object CreatePassiveSellOfferLineFull extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_LINE_FULL)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because there was an insufficient balance of the asset being offered to meet the offer.
  */
case object CreatePassiveSellOfferUnderfunded extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_UNDERFUNDED)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because it would have matched with an offer from the same account.
  */
case object CreatePassiveSellOfferCrossSelf extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_CROSS_SELF)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because there is no issuer for the asset being offered.
  */
case object CreatePassiveSellOfferSellNoIssuer extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_SELL_NO_ISSUER)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because there is no issuer for the asset being sought.
  */
case object CreatePassiveSellOfferBuyNoIssuer extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_BUY_NO_ISSUER)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because it was an update attempt, but an offer with the given id did not exist.
  */
case object UpdatePassiveOfferIdNotFound extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_NOT_FOUND)
    .build()
}

/**
  * CreatePassiveSellOffer operation failed because the cumulative amount of it & all current offers from the same account
  * would exceed the account's available balance.
  */
case object CreatePassiveSellOfferLowReserve extends CreatePassiveSellOfferResult {
  val result: ManageSellOfferResult = new ManageSellOfferResult.Builder()
    .discriminant(ManageSellOfferResultCode.MANAGE_SELL_OFFER_LOW_RESERVE)
    .build()
}
package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.xdr.Decode

sealed abstract class CreatePassiveSellOfferResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 4)

object CreatePassiveSellOfferResult extends Decode {
  val decode: State[Seq[Byte], CreatePassiveSellOfferResult] = int.map {
    case 0 => CreatePassiveSellOfferSuccess
    case -1 => CreatePassiveSellOfferMalformed
    case -2 => CreatePassiveSellOfferSellNoTrust
    case -3 => CreatePassiveSellOfferBuyNoTrust
    case -4 => CreatePassiveSellOfferSellNoAuth
    case -5 => CreatePassiveSellOfferBuyNoAuth
    case -6 => CreatePassiveSellOfferLineFull
    case -7 => CreatePassiveSellOfferUnderfunded
    case -8 => CreatePassiveSellOfferCrossSelf
    case -9 => CreatePassiveSellOfferSellNoIssuer
    case -10 => CreatePassiveSellOfferBuyNoIssuer
    case -11 => UpdatePassiveOfferIdNotFound
    case -12 => CreatePassiveSellOfferLowReserve
  }
}

/**
  * CreatePassiveSellOffer operation was successful.
  */
case object CreatePassiveSellOfferSuccess extends CreatePassiveSellOfferResult(0)

/**
  * CreatePassiveSellOffer operation failed because the request was malformed.
  * E.g. Either of the assets were invalid, the assets were the same as each other,
  * the amount was less than zero, or the price numerator or denominator were zero or less.
  */
case object CreatePassiveSellOfferMalformed extends CreatePassiveSellOfferResult(-1)

/**
  * CreatePassiveSellOffer operation failed because there was no trustline for what was being offered.
  * (This also implies the account was underfunded).
  */
case object CreatePassiveSellOfferSellNoTrust extends CreatePassiveSellOfferResult(-2)

/**
  * CreatePassiveSellOffer operation failed because there was no trustline for what was being sought.
  */
case object CreatePassiveSellOfferBuyNoTrust extends CreatePassiveSellOfferResult(-3)

/**
  * CreatePassiveSellOffer operation failed because the account is not authorised to sell the offered asset.
  */
case object CreatePassiveSellOfferSellNoAuth extends CreatePassiveSellOfferResult(-4)

/**
  * CreatePassiveSellOffer operation failed because the account is not authorised to buy the sought asset.
  */
case object CreatePassiveSellOfferBuyNoAuth extends CreatePassiveSellOfferResult(-5)

/**
  * CreatePassiveSellOffer operation failed because it would have put the account's balance over the limit for the sought asset.
  */
case object CreatePassiveSellOfferLineFull extends CreatePassiveSellOfferResult(-6)

/**
  * CreatePassiveSellOffer operation failed because there was an insufficient balance of the asset being offered to meet the offer.
  */
case object CreatePassiveSellOfferUnderfunded extends CreatePassiveSellOfferResult(-7)

/**
  * CreatePassiveSellOffer operation failed because it would have matched with an offer from the same account.
  */
case object CreatePassiveSellOfferCrossSelf extends CreatePassiveSellOfferResult(-8)

/**
  * CreatePassiveSellOffer operation failed because there is no issuer for the asset being offered.
  */
case object CreatePassiveSellOfferSellNoIssuer extends CreatePassiveSellOfferResult(-9)

/**
  * CreatePassiveSellOffer operation failed because there is no issuer for the asset being sought.
  */
case object CreatePassiveSellOfferBuyNoIssuer extends CreatePassiveSellOfferResult(-10)

/**
  * CreatePassiveSellOffer operation failed because it was an update attempt, but an offer with the given id did not exist.
  */
case object UpdatePassiveOfferIdNotFound extends CreatePassiveSellOfferResult(-11)

/**
  * CreatePassiveSellOffer operation failed because the cumulative amount of it & all current offers from the same account
  * would exceed the account's available balance.
  */
case object CreatePassiveSellOfferLowReserve extends CreatePassiveSellOfferResult(-12)
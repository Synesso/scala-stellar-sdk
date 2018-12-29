package stellar.sdk.res

import cats.data.State
import stellar.sdk.xdr.Decode

sealed abstract class CreatePassiveOfferResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 4)

object CreatePassiveOfferResult {
  val decode: State[Seq[Byte], CreatePassiveOfferResult] = Decode.int.map {
    case 0 => CreatePassiveOfferSuccess
    case -1 => CreatePassiveOfferMalformed
    case -2 => CreatePassiveOfferSellNoTrust
    case -3 => CreatePassiveOfferBuyNoTrust
    case -4 => CreatePassiveOfferSellNoAuth
    case -5 => CreatePassiveOfferBuyNoAuth
    case -6 => CreatePassiveOfferLineFull
    case -7 => CreatePassiveOfferUnderfunded
    case -8 => CreatePassiveOfferCrossSelf
    case -9 => CreatePassiveOfferSellNoIssuer
    case -10 => CreatePassiveOfferBuyNoIssuer
    case -11 => UpdatePassiveOfferIdNotFound
    case -12 => CreatePassiveOfferLowReserve
  }
}

/**
  * CreatePassiveOffer operation was successful.
  */
case object CreatePassiveOfferSuccess extends CreatePassiveOfferResult(0)

/**
  * CreatePassiveOffer operation failed because the request was malformed.
  * E.g. Either of the assets were invalid, the assets were the same as each other,
  * the amount was less than zero, or the price numerator or denominator were zero or less.
  */
case object CreatePassiveOfferMalformed extends CreatePassiveOfferResult(-1)

/**
  * CreatePassiveOffer operation failed because there was no trustline for what was being offered.
  * (This also implies the account was underfunded).
  */
case object CreatePassiveOfferSellNoTrust extends CreatePassiveOfferResult(-2)

/**
  * CreatePassiveOffer operation failed because there was no trustline for what was being sought.
  */
case object CreatePassiveOfferBuyNoTrust extends CreatePassiveOfferResult(-3)

/**
  * CreatePassiveOffer operation failed because the account is not authorised to sell the offered asset.
  */
case object CreatePassiveOfferSellNoAuth extends CreatePassiveOfferResult(-4)

/**
  * CreatePassiveOffer operation failed because the account is not authorised to buy the sought asset.
  */
case object CreatePassiveOfferBuyNoAuth extends CreatePassiveOfferResult(-5)

/**
  * CreatePassiveOffer operation failed because it would have put the account's balance over the limit for the sought asset.
  */
case object CreatePassiveOfferLineFull extends CreatePassiveOfferResult(-6)

/**
  * CreatePassiveOffer operation failed because there was an insufficient balance of the asset being offered to meet the offer.
  */
case object CreatePassiveOfferUnderfunded extends CreatePassiveOfferResult(-7)

/**
  * CreatePassiveOffer operation failed because it would have matched with an offer from the same account.
  */
case object CreatePassiveOfferCrossSelf extends CreatePassiveOfferResult(-8)

/**
  * CreatePassiveOffer operation failed because there is no issuer for the asset being offered.
  */
case object CreatePassiveOfferSellNoIssuer extends CreatePassiveOfferResult(-9)

/**
  * CreatePassiveOffer operation failed because there is no issuer for the asset being sought.
  */
case object CreatePassiveOfferBuyNoIssuer extends CreatePassiveOfferResult(-10)

/**
  * CreatePassiveOffer operation failed because it was an update attempt, but an offer with the given id did not exist.
  */
case object UpdatePassiveOfferIdNotFound extends CreatePassiveOfferResult(-11)

/**
  * CreatePassiveOffer operation failed because the cumulative amount of it & all current offers from the same account
  * would exceed the account's available balance.
  */
case object CreatePassiveOfferLowReserve extends CreatePassiveOfferResult(-12)
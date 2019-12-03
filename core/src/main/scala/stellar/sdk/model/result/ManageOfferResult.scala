package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.ledger.OfferEntry
import stellar.sdk.model.xdr.{Decode, Encode}

sealed abstract class ManageOfferResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 3)

object ManageOfferResult extends Decode {
  val decode: State[Seq[Byte], ManageOfferResult] = int.flatMap {
    case 0 => for {
      claims <- arr(OfferClaim.decode)
      entry <- switch[Option[OfferEntry]](
        OfferEntry.decode.map(Some(_: OfferEntry)),
        OfferEntry.decode.map(Some(_: OfferEntry)),
        State.pure(Option.empty[OfferEntry])
      )
    } yield ManageOfferSuccess(claims, entry)
    case -1 => State.pure(ManageOfferMalformed)
    case -2 => State.pure(ManageOfferSellNoTrust)
    case -3 => State.pure(ManageOfferBuyNoTrust)
    case -4 => State.pure(ManageOfferSellNoAuth)
    case -5 => State.pure(ManageOfferBuyNoAuth)
    case -6 => State.pure(ManageOfferLineFull)
    case -7 => State.pure(ManageOfferUnderfunded)
    case -8 => State.pure(ManageOfferCrossSelf)
    case -9 => State.pure(ManageOfferSellNoIssuer)
    case -10 => State.pure(ManageOfferBuyNoIssuer)
    case -11 => State.pure(UpdateOfferIdNotFound)
    case -12 => State.pure(ManageOfferLowReserve)
  }
}

/**
  * ManageOffer operation was successful.
  *
  * @param claims the trades that were effected as a result of posting this offer.
  * @param entry the offer entry that was newly created or updated.
  */
case class ManageOfferSuccess(claims: Seq[OfferClaim], entry: Option[OfferEntry]) extends ManageOfferResult(0) {
  override def encode: Stream[Byte] = super.encode ++ Encode.arr(claims) ++ Encode.opt(entry, ifPresent = 0, ifAbsent = 2)
}

/**
  * ManageOffer operation failed because the request was malformed.
  * E.g. Either of the assets were invalid, the assets were the same as each other,
  * the amount was less than zero, or the price numerator or denominator were zero or less.
  */
case object ManageOfferMalformed extends ManageOfferResult(-1)

/**
  * ManageOffer operation failed because there was no trustline for what was being offered.
  * (This also implies the account was underfunded).
  */
case object ManageOfferSellNoTrust extends ManageOfferResult(-2)

/**
  * ManageOffer operation failed because there was no trustline for what was being sought.
  */
case object ManageOfferBuyNoTrust extends ManageOfferResult(-3)

/**
  * ManageOffer operation failed because the account is not authorised to sell the offered asset.
  */
case object ManageOfferSellNoAuth extends ManageOfferResult(-4)

/**
  * ManageOffer operation failed because the account is not authorised to buy the sought asset.
  */
case object ManageOfferBuyNoAuth extends ManageOfferResult(-5)

/**
  * ManageOffer operation failed because it would have put the account's balance over the limit for the sought asset.
  */
case object ManageOfferLineFull extends ManageOfferResult(-6)

/**
  * ManageOffer operation failed because there was an insufficient balance of the asset being offered to meet the offer.
  */
case object ManageOfferUnderfunded extends ManageOfferResult(-7)

/**
  * ManageOffer operation failed because it would have matched with an offer from the same account.
  */
case object ManageOfferCrossSelf extends ManageOfferResult(-8)

/**
  * ManageOffer operation failed because there is no issuer for the asset being offered.
  */
case object ManageOfferSellNoIssuer extends ManageOfferResult(-9)

/**
  * ManageOffer operation failed because there is no issuer for the asset being sought.
  */
case object ManageOfferBuyNoIssuer extends ManageOfferResult(-10)

/**
  * ManageOffer operation failed because it was an update attempt, but an offer with the given id did not exist.
  */
case object UpdateOfferIdNotFound extends ManageOfferResult(-11)

/**
  * ManageOffer operation failed because the cumulative amount of it & all current offers from the same account
  * would exceed the account's available balance.
  */
case object ManageOfferLowReserve extends ManageOfferResult(-12)



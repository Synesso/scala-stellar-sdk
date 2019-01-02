package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.{Amount, Asset}
import stellar.sdk.{KeyPair, PublicKey}
import stellar.sdk.model.xdr.{Decode, Encode}

sealed abstract class PathPaymentResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 2)

object PathPaymentResult {
  val decode: State[Seq[Byte], PathPaymentResult] = Decode.int.flatMap {
    case 0 => Decode.arr(OfferClaim.decode).flatMap { claims =>
      KeyPair.decode.flatMap { destination =>
        Amount.decode.map(PathPaymentSuccess(claims, destination, _))
      }
    }
    case -1 => State.pure(PathPaymentMalformed)
    case -2 => State.pure(PathPaymentUnderfunded)
    case -3 => State.pure(PathPaymentSourceNoTrust)
    case -4 => State.pure(PathPaymentSourceNotAuthorised)
    case -5 => State.pure(PathPaymentNoDestination)
    case -6 => State.pure(PathPaymentDestinationNoTrust)
    case -7 => State.pure(PathPaymentDestinationNotAuthorised)
    case -8 => State.pure(PathPaymentDestinationLineFull)
    case -9 => Asset.decode.map(PathPaymentNoIssuer)
    case -10 => State.pure(PathPaymentTooFewOffers)
    case -11 => State.pure(PathPaymentOfferCrossesSelf)
    case -12 => State.pure(PathPaymentSendMaxExceeded)
  }
}

/**
  * PathPayment operation was successful.
  * @param claims the trades that were effected during this path payment.
  */
case class PathPaymentSuccess(claims: Seq[OfferClaim],
                              destination: PublicKey,
                              paid: Amount) extends PathPaymentResult(0) {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.arr(claims) ++
      destination.encode ++
      paid.encode

}

/**
  * PathPayment operation failed because the request was malformed.
  * E.g. The destination or sendMax amounts were negative, or the any of the asset were invalid.
  */
case object PathPaymentMalformed extends PathPaymentResult(-1)

/**
  * PathPayment operation failed because there were insufficient funds in the source account.
  */
case object PathPaymentUnderfunded extends PathPaymentResult(-2)

/**
  * PathPayment operation failed because the sender has not trustline for the specified asset.
  * (Additionally, this implies the sender doesn't have the funds to send anyway).
  */
case object PathPaymentSourceNoTrust extends PathPaymentResult(-3)

/**
  * PathPayment operation failed because the sender is not authorised to send the specified asset.
  */
case object PathPaymentSourceNotAuthorised extends PathPaymentResult(-4)

/**
  * PathPayment operation failed because the destination account did not exist.
  */
case object PathPaymentNoDestination extends PathPaymentResult(-5)

/**
  * PathPayment operation failed because the destination account does not have a trustline for the asset.
  */
case object PathPaymentDestinationNoTrust extends PathPaymentResult(-6)

/**
  * PathPayment operation failed because the destination account is not authorised to hold the asset.
  */
case object PathPaymentDestinationNotAuthorised extends PathPaymentResult(-7)

/**
  * PathPayment operation failed because it would have put the destination account's balance over the limit for the asset.
  */
case object PathPaymentDestinationLineFull extends PathPaymentResult(-8)

/**
  * PathPayment operation failed because there was no issuer for one or more of the assets.
  * @param asset the asset for which there's no issuer.
  */
case class PathPaymentNoIssuer(asset: Asset) extends PathPaymentResult(-9) {
  override def encode: Stream[Byte] = super.encode ++ asset.encode
}

/**
  * PathPayment operation failed because there were too few offers to satisfy the path.
  */
case object PathPaymentTooFewOffers extends PathPaymentResult(-10)

/**
  * PathPayment operation failed because it would have resulted in matching its own offer.
  */
case object PathPaymentOfferCrossesSelf extends PathPaymentResult(-11)

/**
  * PathPayment operation failed because it could not be effected without sending more than the specified maximum.
  */
case object PathPaymentSendMaxExceeded extends PathPaymentResult(-12)
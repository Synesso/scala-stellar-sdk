package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.xdr.Decode

sealed abstract class PaymentResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 1)

object PaymentResult extends Decode {
  val decode: State[Seq[Byte], PaymentResult] = int.map {
    case 0 => PaymentSuccess
    case -1 => PaymentMalformed
    case -2 => PaymentUnderfunded
    case -3 => PaymentSourceNoTrust
    case -4 => PaymentSourceNotAuthorised
    case -5 => PaymentNoDestination
    case -6 => PaymentDestinationNoTrust
    case -7 => PaymentDestinationNotAuthorised
    case -8 => PaymentDestinationLineFull
    case -9 => PaymentNoIssuer
  }
}

/**
  * Payment operation was successful.
  */
case object PaymentSuccess extends PaymentResult(0)

/**
  * Payment operation failed because the request was malformed.
  * E.g. The amount was negative, or the asset was invalid.
  */
case object PaymentMalformed extends PaymentResult(-1)

/**
  * Payment operation failed because there were insufficient funds.
  */
case object PaymentUnderfunded extends PaymentResult(-2)

/**
  * Payment operation failed because the sender has not trustline for the specified asset.
  * (Additionally, this implies the sender doesn't have the funds to send anyway).
  */
case object PaymentSourceNoTrust extends PaymentResult(-3)

/**
  * Payment operation failed because the sender is not authorised to send the specified asset.
  */
case object PaymentSourceNotAuthorised extends PaymentResult(-4)

/**
  * Payment operation failed because the destination account did not exist.
  */
case object PaymentNoDestination extends PaymentResult(-5)

/**
  * Payment operation failed because the destination account does not have a trustline for the asset.
  */
case object PaymentDestinationNoTrust extends PaymentResult(-6)

/**
  * Payment operation failed because the destination account is not authorised to hold the asset.
  */
case object PaymentDestinationNotAuthorised extends PaymentResult(-7)

/**
  * Payment operation failed because it would have put the destination account's balance over the limit for the asset.
  */
case object PaymentDestinationLineFull extends PaymentResult(-8)

/**
  * Payment operation failed because there was no issuer specified for the asset.
  */
case object PaymentNoIssuer extends PaymentResult(-9)
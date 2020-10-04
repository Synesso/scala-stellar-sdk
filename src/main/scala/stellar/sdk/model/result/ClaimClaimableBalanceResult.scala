package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.ClaimableBalanceId
import stellar.sdk.model.xdr.Decode

sealed abstract class ClaimClaimableBalanceResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 15)

object ClaimClaimableBalanceResult extends Decode {
  val decode: State[Seq[Byte], ClaimClaimableBalanceResult] = int.map {
    case 0 => ClaimClaimableBalanceSuccess
    case -1 => ClaimClaimableBalanceDoesNotExist
    case -2 => ClaimClaimableBalanceCannotClaim
    case -3 => ClaimClaimableBalanceLineFull
    case -4 => ClaimClaimableBalanceNoTrust
    case -5 => ClaimClaimableBalanceNotAuthorized
  }
}

/**
 * ClaimClaimableBalance operation was successful.
 */
case object ClaimClaimableBalanceSuccess extends ClaimClaimableBalanceResult(0)

/**
 * ClaimClaimableBalance operation failed because the balance did not exist
 */
case object ClaimClaimableBalanceDoesNotExist extends ClaimClaimableBalanceResult(-1)

/**
 * ClaimClaimableBalance operation failed because the balance could not be claimed
 */
case object ClaimClaimableBalanceCannotClaim extends ClaimClaimableBalanceResult(-2)

/**
 * ClaimClaimableBalance operation failed because the trustline is full
 */
case object ClaimClaimableBalanceLineFull extends ClaimClaimableBalanceResult(-3)

/**
 * ClaimClaimableBalance operation failed because the required trustline is not present
 */
case object ClaimClaimableBalanceNoTrust extends ClaimClaimableBalanceResult(-4)

/**
 * ClaimClaimableBalance operation failed because the requester was not authorised
 */
case object ClaimClaimableBalanceNotAuthorized extends ClaimClaimableBalanceResult(-5)
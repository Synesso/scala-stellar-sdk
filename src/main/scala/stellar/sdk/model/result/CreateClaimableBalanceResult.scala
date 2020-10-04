package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.ClaimableBalanceId
import stellar.sdk.model.xdr.Decode

sealed abstract class CreateClaimableBalanceResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 14)

object CreateClaimableBalanceResult extends Decode {
  val decode: State[Seq[Byte], CreateClaimableBalanceResult] = int.flatMap {
    case 0 => ClaimableBalanceId.decode.map(CreateClaimableBalanceSuccess)
    case -1 => State.pure(CreateClaimableBalanceMalformed)
    case -2 => State.pure(CreateClaimableBalanceLowReserve)
    case -3 => State.pure(CreateClaimableBalanceNoTrust)
    case -4 => State.pure(CreateClaimableBalanceNotAuthorized)
    case -5 => State.pure(CreateClaimableBalanceUnderfunded)
  }
}

/**
 * CreateClaimableBalance operation was successful.
 * @param id the identifier of the claimable balance created.
 */
case class CreateClaimableBalanceSuccess(id: ClaimableBalanceId) extends CreateClaimableBalanceResult(0) {
  override def encode: LazyList[Byte] = super.encode ++ id.encode
}

/**
 * CreateClaimableBalance operation failed because the request was malformed
 */
case object CreateClaimableBalanceMalformed extends CreateClaimableBalanceResult(-1)

/**
 * CreateClaimableBalance operation failed because the declared reserve was insufficient
 */
case object CreateClaimableBalanceLowReserve extends CreateClaimableBalanceResult(-2)

/**
 * CreateClaimableBalance operation failed because the required trustline does not exist
 */
case object CreateClaimableBalanceNoTrust extends CreateClaimableBalanceResult(-3)

/**
 * CreateClaimableBalance operation failed because the account was not authorized for this asset
 */
case object CreateClaimableBalanceNotAuthorized extends CreateClaimableBalanceResult(-4)

/**
 * CreateClaimableBalance operation failed because the source account had insufficient funds
 */
case object CreateClaimableBalanceUnderfunded extends CreateClaimableBalanceResult(-5)
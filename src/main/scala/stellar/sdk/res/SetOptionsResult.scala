package stellar.sdk.res

import cats.data.State
import stellar.sdk.xdr.Decode

sealed abstract class SetOptionsResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 5)

object SetOptionsResult {
  val decode: State[Seq[Byte], SetOptionsResult] = Decode.int.map {
    case 0 => SetOptionsSuccess
    case -1 => SetOptionsLowReserve
    case -2 => SetOptionsTooManySigners
    case -3 => SetOptionsBadFlags
    case -4 => SetOptionsInvalidInflation
    case -5 => SetOptionsCannotChange
    case -6 => SetOptionsUnknownFlag
    case -7 => SetOptionsThresholdOutOfRange
    case -8 => SetOptionsBadSigner
    case -9 => SetOptionsInvalidHomeDomain
  }
}

/**
  * SetOptions operation was successful.
  */
case object SetOptionsSuccess extends SetOptionsResult(0)

/**
  * SetOptions operation failed because there was insufficient reserve funds to add another signer.
  */
case object SetOptionsLowReserve extends SetOptionsResult(-1)

/**
  * SetOptions operation failed because the maximum number of signers has already been met.
  */
case object SetOptionsTooManySigners extends SetOptionsResult(-2)

/**
  * SetOptions operation failed because there was an invalid combination of set/clear flags.
  */
case object SetOptionsBadFlags extends SetOptionsResult(-3)

/**
  * SetOptions operation failed because the inflation target does not exist.
  */
case object SetOptionsInvalidInflation extends SetOptionsResult(-4)

/**
  * SetOptions operation failed because the options can no longer be altered.
  */
case object SetOptionsCannotChange extends SetOptionsResult(-5)

/**
  * SetOptions operation failed because the flag being altered does not exist.
  */
case object SetOptionsUnknownFlag extends SetOptionsResult(-6)

/**
  * SetOptions operation failed because a bad value for a weight/threshold was provided.
  */
case object SetOptionsThresholdOutOfRange extends SetOptionsResult(-7)

/**
  * SetOptions operation failed because of an attempt to set the master key as a signer.
  */
case object SetOptionsBadSigner extends SetOptionsResult(-8)

/**
  * SetOptions operation failed because the home domain was invalid.
  */
case object SetOptionsInvalidHomeDomain extends SetOptionsResult(-9)

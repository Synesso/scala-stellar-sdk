package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{OperationType, SetOptionsResultCode, SetOptionsResult => XSetOptionsResult}

sealed abstract class SetOptionsResult extends ProcessedOperationResult {
  def result: XSetOptionsResult
  override def transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.SET_OPTIONS)
    .setOptionsResult(result)
    .build()
}

object SetOptionsResult {
  def decodeXdr(xdr: XSetOptionsResult): SetOptionsResult = xdr.getDiscriminant match {
    case SetOptionsResultCode.SET_OPTIONS_SUCCESS => SetOptionsSuccess
    case SetOptionsResultCode.SET_OPTIONS_LOW_RESERVE => SetOptionsLowReserve
    case SetOptionsResultCode.SET_OPTIONS_TOO_MANY_SIGNERS => SetOptionsTooManySigners
    case SetOptionsResultCode.SET_OPTIONS_BAD_FLAGS => SetOptionsBadFlags
    case SetOptionsResultCode.SET_OPTIONS_INVALID_INFLATION => SetOptionsInvalidInflation
    case SetOptionsResultCode.SET_OPTIONS_CANT_CHANGE => SetOptionsCannotChange
    case SetOptionsResultCode.SET_OPTIONS_UNKNOWN_FLAG => SetOptionsUnknownFlag
    case SetOptionsResultCode.SET_OPTIONS_THRESHOLD_OUT_OF_RANGE => SetOptionsThresholdOutOfRange
    case SetOptionsResultCode.SET_OPTIONS_BAD_SIGNER => SetOptionsBadSigner
    case SetOptionsResultCode.SET_OPTIONS_INVALID_HOME_DOMAIN => SetOptionsInvalidHomeDomain
  }
}

/**
 * SetOptions operation was successful.
 */
case object SetOptionsSuccess extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_SUCCESS)
    .build()
}

/**
 * SetOptions operation failed because there was insufficient reserve funds to add another signer.
 */
case object SetOptionsLowReserve extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_LOW_RESERVE)
    .build()
}

/**
 * SetOptions operation failed because the maximum number of signers has already been met.
 */
case object SetOptionsTooManySigners extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_TOO_MANY_SIGNERS)
    .build()
}

/**
 * SetOptions operation failed because there was an invalid combination of set/clear flags.
 */
case object SetOptionsBadFlags extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_BAD_FLAGS)
    .build()
}

/**
 * SetOptions operation failed because the inflation target does not exist.
 */
case object SetOptionsInvalidInflation extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_INVALID_INFLATION)
    .build()
}

/**
 * SetOptions operation failed because the options can no longer be altered.
 */
case object SetOptionsCannotChange extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_CANT_CHANGE)
    .build()
}

/**
 * SetOptions operation failed because the flag being altered does not exist.
 */
case object SetOptionsUnknownFlag extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_UNKNOWN_FLAG)
    .build()
}

/**
 * SetOptions operation failed because a bad value for a weight/threshold was provided.
 */
case object SetOptionsThresholdOutOfRange extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_THRESHOLD_OUT_OF_RANGE)
    .build()
}

/**
 * SetOptions operation failed because of an attempt to set the master key as a signer.
 */
case object SetOptionsBadSigner extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_BAD_SIGNER)
    .build()
}

/**
 * SetOptions operation failed because the home domain was invalid.
 */
case object SetOptionsInvalidHomeDomain extends SetOptionsResult {
  override def result: XSetOptionsResult = new XSetOptionsResult.Builder()
    .discriminant(SetOptionsResultCode.SET_OPTIONS_INVALID_HOME_DOMAIN)
    .build()
}
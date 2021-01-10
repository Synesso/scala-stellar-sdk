package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{CreateAccountResultCode, OperationType, CreateAccountResult => XCreateAccountResult}

sealed abstract class CreateAccountResult extends ProcessedOperationResult {
  val result: XCreateAccountResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.CREATE_ACCOUNT)
    .createAccountResult(result)
    .build()
}

object CreateAccountResult {
}

/**
 * CreateAccount operation was successful.
 */
case object CreateAccountSuccess extends CreateAccountResult {
  val result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CreateAccountResultCode.CREATE_ACCOUNT_SUCCESS)
    .build()
}

/**
 * CreateAccount operation failed because the destination account was malformed.
 */
case object CreateAccountMalformed extends CreateAccountResult {
  val result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CreateAccountResultCode.CREATE_ACCOUNT_MALFORMED)
    .build()
}

/**
 * CreateAccount operation failed because there was insufficient funds in the source account.
 */
case object CreateAccountUnderfunded extends CreateAccountResult {
  val result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CreateAccountResultCode.CREATE_ACCOUNT_UNDERFUNDED)
    .build()
}

/**
 * CreateAccount operation failed because there was insufficient funds sent to cover the base reserve.
 */
case object CreateAccountLowReserve extends CreateAccountResult {
  val result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CreateAccountResultCode.CREATE_ACCOUNT_LOW_RESERVE)
    .build()
}

/**
 * CreateAccount operation failed because the destination account already exists.
 */
case object CreateAccountAlreadyExists extends CreateAccountResult {
  val result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CreateAccountResultCode.CREATE_ACCOUNT_ALREADY_EXIST)
    .build()
}

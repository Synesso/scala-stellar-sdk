package stellar.sdk.model.result

import org.stellar.xdr.CreateAccountResultCode._
import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{OperationType, CreateAccountResult => XCreateAccountResult}

sealed abstract class CreateAccountResult extends ProcessedOperationResult {
  def result: XCreateAccountResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.CREATE_ACCOUNT)
    .createAccountResult(result)
    .build()
}

object CreateAccountResult {
  def decodeXdr(xdr: XCreateAccountResult): CreateAccountResult = xdr.getDiscriminant match {
    case CREATE_ACCOUNT_SUCCESS => CreateAccountSuccess
    case CREATE_ACCOUNT_MALFORMED => CreateAccountMalformed
    case CREATE_ACCOUNT_UNDERFUNDED => CreateAccountUnderfunded
    case CREATE_ACCOUNT_LOW_RESERVE => CreateAccountLowReserve
    case CREATE_ACCOUNT_ALREADY_EXIST => CreateAccountAlreadyExists
  }
}

/**
 * CreateAccount operation was successful.
 */
case object CreateAccountSuccess extends CreateAccountResult {
  override def result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CREATE_ACCOUNT_SUCCESS)
    .build()
}

/**
 * CreateAccount operation failed because the destination account was malformed.
 */
case object CreateAccountMalformed extends CreateAccountResult {
  override def result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CREATE_ACCOUNT_MALFORMED)
    .build()
}

/**
 * CreateAccount operation failed because there was insufficient funds in the source account.
 */
case object CreateAccountUnderfunded extends CreateAccountResult {
  override def result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CREATE_ACCOUNT_UNDERFUNDED)
    .build()
}

/**
 * CreateAccount operation failed because there was insufficient funds sent to cover the base reserve.
 */
case object CreateAccountLowReserve extends CreateAccountResult {
  override def result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CREATE_ACCOUNT_LOW_RESERVE)
    .build()
}

/**
 * CreateAccount operation failed because the destination account already exists.
 */
case object CreateAccountAlreadyExists extends CreateAccountResult {
  override def result: XCreateAccountResult = new XCreateAccountResult.Builder()
    .discriminant(CREATE_ACCOUNT_ALREADY_EXIST)
    .build()
}
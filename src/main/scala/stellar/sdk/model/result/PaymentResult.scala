package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{OperationType, PaymentResultCode, PaymentResult => XPaymentResult}

sealed abstract class PaymentResult extends ProcessedOperationResult {
  val result: XPaymentResult
  val transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.PAYMENT)
    .paymentResult(result)
    .build()
}

object PaymentResult {
}

/**
  * Payment operation was successful.
  */
case object PaymentSuccess extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_SUCCESS)
    .build()
}

/**
  * Payment operation failed because the request was malformed.
  * E.g. The amount was negative, or the asset was invalid.
  */
case object PaymentMalformed extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_MALFORMED)
    .build()
}

/**
  * Payment operation failed because there were insufficient funds.
  */
case object PaymentUnderfunded extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_UNDERFUNDED)
    .build()
}

/**
  * Payment operation failed because the sender has not trustline for the specified asset.
  * (Additionally, this implies the sender doesn't have the funds to send anyway).
  */
case object PaymentSourceNoTrust extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_SRC_NO_TRUST)
    .build()
}

/**
  * Payment operation failed because the sender is not authorised to send the specified asset.
  */
case object PaymentSourceNotAuthorised extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_SRC_NOT_AUTHORIZED)
    .build()
}

/**
  * Payment operation failed because the destination account did not exist.
  */
case object PaymentNoDestination extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_NO_DESTINATION)
    .build()
}

/**
  * Payment operation failed because the destination account does not have a trustline for the asset.
  */
case object PaymentDestinationNoTrust extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_NO_TRUST)
    .build()
}

/**
  * Payment operation failed because the destination account is not authorised to hold the asset.
  */
case object PaymentDestinationNotAuthorised extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_NOT_AUTHORIZED)
    .build()
}

/**
  * Payment operation failed because it would have put the destination account's balance over the limit for the asset.
  */
case object PaymentDestinationLineFull extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_LINE_FULL)
    .build()
}

/**
  * Payment operation failed because there was no issuer specified for the asset.
  */
case object PaymentNoIssuer extends PaymentResult {
  val result: XPaymentResult = new XPaymentResult.Builder()
    .discriminant(PaymentResultCode.PAYMENT_NO_ISSUER)
    .build()
}
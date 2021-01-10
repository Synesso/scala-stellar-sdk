package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{OperationResultCode, OperationResult => XOperationResult}
/**
  * The result of an operation previously submitted to the network.
  */
abstract class OperationResult {
  def xdr: XOperationResult
}

/**
  * The result of an operation previously submitted to, and attempted to be processed by the network.
  */
abstract class ProcessedOperationResult extends OperationResult {
  val transactionResult: OperationResultTr
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opINNER)
    .tr(transactionResult)
  .build()

}

object OperationResult {
}

/**
  * The operation was not attempted, because there were too few valid signatures, or the wrong network was used.
  */
case object BadAuthenticationResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opBAD_AUTH)
    .build()
}

/**
  * The operation was not attempted, because the source account was not found.
  */
case object NoSourceAccountResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opNO_ACCOUNT)
    .build()
}

/**
  * The operation was not attempted, because the requested operation is not supported by the network.
  */
case object OperationNotSupportedResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opNOT_SUPPORTED)
    .build()
}
package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.INFLATION
import stellar.sdk.PublicKeyOps

/**
  * Requests that the network runs the inflation process.
  */
case class InflationOperation(sourceAccount: Option[PublicKeyOps] = None) extends Operation {
  override val toOperationBody: OperationBody = {
    val body = new OperationBody
    body.setDiscriminant(INFLATION)
    body
  }
}

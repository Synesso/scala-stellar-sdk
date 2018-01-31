package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.INFLATION
import stellar.sdk.KeyPair

case object InflationOperation extends Operation {
  override val toOperationBody: OperationBody = {
    val body = new OperationBody
    body.setDiscriminant(INFLATION)
    body
  }
}

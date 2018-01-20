package stellar.scala.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.INFLATION
import stellar.scala.sdk.KeyPair

case object InflationOperation extends Operation {
  override val sourceAccount: Option[KeyPair] = None
  override val toOperationBody: OperationBody = {
    val body = new OperationBody
    body.setDiscriminant(INFLATION)
    body
  }
}

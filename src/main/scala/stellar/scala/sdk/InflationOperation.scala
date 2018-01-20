package stellar.scala.sdk
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.INFLATION

case object InflationOperation extends Operation {
  override val sourceAccount: Option[KeyPair] = None
  override val toOperationBody: OperationBody = {
    val body = new OperationBody
    body.setDiscriminant(INFLATION)
    body
  }
}

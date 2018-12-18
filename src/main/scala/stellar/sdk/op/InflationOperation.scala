package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.INFLATION
import stellar.sdk.{Encode, PublicKeyOps}

/**
  * Requests that the network runs the inflation process.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#inflation endpoint doc]]
  */
case class InflationOperation(sourceAccount: Option[PublicKeyOps] = None) extends Operation {
  override val toOperationBody: OperationBody = {
    val body = new OperationBody
    body.setDiscriminant(INFLATION)
    body
  }

  override def encode: Stream[Byte] = Encode.int(9)
}

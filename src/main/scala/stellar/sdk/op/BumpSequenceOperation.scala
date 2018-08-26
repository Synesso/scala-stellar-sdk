package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import stellar.sdk.PublicKeyOps

/**
  * Bumps forward the sequence number of the source account of the operation, allowing it to invalidate any transactions
  * with a smaller sequence number.
  *
  * @param bumpTo the number to increase the sequence number to
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#bump-sequence endpoint doc]]
  */
case class BumpSequenceOperation(bumpTo: Long,
                                 sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def toOperationBody: OperationBody = ???
}

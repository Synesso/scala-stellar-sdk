package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.BUMP_SEQUENCE
import org.stellar.sdk.xdr.{BumpSequenceOp, SequenceNumber}
import stellar.sdk.XDRPrimitives.int64
import stellar.sdk.{PublicKey, PublicKeyOps}

import scala.util.Try

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

  override def toOperationBody: OperationBody = {
    val body = new OperationBody
    body.setBumpSequenceOp(new BumpSequenceOp)
    body.getBumpSequenceOp.setBumpTo(new SequenceNumber)
    body.getBumpSequenceOp.getBumpTo.setSequenceNumber(int64(bumpTo))
    body.setDiscriminant(BUMP_SEQUENCE)
    body
  }
}

object BumpSequenceOperation {
  def from(op: BumpSequenceOp, source: Option[PublicKey]): Try[Operation] = Try {
    BumpSequenceOperation(op.getBumpTo.getSequenceNumber.getInt64, source)
  }
}

package stellar.scala.sdk

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.CHANGE_TRUST
import org.stellar.sdk.xdr.{ChangeTrustOp, Int64}

import scala.util.Try

/**
  * Represents <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html#change-trust" target="_blank">ChangeTrust</a> operation.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html" target="_blank">List of Operations</a>
  */
case class ChangeTrustOperation(limit: Amount, sourceAccount: Option[KeyPair] = None) extends Operation {
  override def toOperationBody: OperationBody = {
    val op = new ChangeTrustOp
    op.setLimit(new Int64)
    op.getLimit.setInt64(limit.units)
    op.setLine(limit.asset.toXDR)
    val body = new OperationBody
    body.setDiscriminant(CHANGE_TRUST)
    body.setChangeTrustOp(op)
    body
  }
}

object ChangeTrustOperation {
  def apply(sourceAccount: KeyPair, limit: Amount): ChangeTrustOperation =
    ChangeTrustOperation(limit, Some(sourceAccount))

  def from(op: ChangeTrustOp): Try[ChangeTrustOperation] = {
    Asset.fromXDR(op.getLine).map(Amount(op.getLimit.getInt64.longValue, _)).map(ChangeTrustOperation(_))
  }
}

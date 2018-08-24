package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.CHANGE_TRUST
import org.stellar.sdk.xdr.{ChangeTrustOp, Int64}
import stellar.sdk.{Amount, Asset, _}

import scala.util.Try

/**
  * Represents <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html#change-trust" target="_blank">ChangeTrust</a> operation.
  *
  * The source account is stating that it will trust the asset of the limit up to the amount of the limit.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html" target="_blank">List of Operations</a>
  */
case class ChangeTrustOperation(limit: IssuedAmount, sourceAccount: Option[PublicKeyOps] = None) extends Operation {
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
  def from(op: ChangeTrustOp, source: Option[PublicKey]): Try[ChangeTrustOperation] = {
    Asset.fromXDR(op.getLine).map(Amount(op.getLimit.getInt64.longValue, _)).map {
      case a: IssuedAmount => ChangeTrustOperation(a, source)
      case _: NativeAmount => throw new IllegalArgumentException("Change trust operation with a native limit")
    }
  }
}

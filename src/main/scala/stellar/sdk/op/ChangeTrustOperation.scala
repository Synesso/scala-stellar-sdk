package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.CHANGE_TRUST
import org.stellar.sdk.xdr.{ChangeTrustOp, Int64}
import stellar.sdk.{Amount, Asset, _}

import scala.util.Try

/**
  * The source account is stating that it will trust the asset of the limit up to the amount of the limit.
  *
  * @param limit the asset to be trusted and the limit of that trust
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#change-trust endpoint doc]]
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

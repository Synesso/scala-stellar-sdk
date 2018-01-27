package stellar.scala.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.PATH_PAYMENT
import org.stellar.sdk.xdr._
import stellar.scala.sdk.{Amount, Asset, KeyPair, PublicKeyOps, TrySeq}

import scala.util.Try

/**
  * Represents <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html#path-payment" target="_blank">PathPayment</a> operation.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html" target="_blank">List of Operations</a>
  */
case class PathPaymentOperation(sendMax: Amount,
                                destinationAccount: PublicKeyOps,
                                destinationAmount: Amount,
                                path: Seq[Asset]) extends Operation {

  override def toOperationBody: OperationBody = {
    val op = new PathPaymentOp
    op.setSendAsset(sendMax.asset.toXDR)
    val sendMaxI = new Int64
    sendMaxI.setInt64(sendMax.units)
    op.setSendMax(sendMaxI)
    val destination = new AccountID
    destination.setAccountID(destinationAccount.getXDRPublicKey)
    op.setDestination(destination)
    op.setDestAsset(destinationAmount.asset.toXDR)
    val destAmountI = new Int64
    destAmountI.setInt64(destinationAmount.units)
    op.setDestAmount(destAmountI)
    op.setPath(path.map(_.toXDR).toArray)
    val body = new OperationBody
    body.setDiscriminant(PATH_PAYMENT)
    body.setPathPaymentOp(op)
    body
  }

}

object PathPaymentOperation extends TrySeq {

  def from(op: PathPaymentOp): Try[PathPaymentOperation] = for {
    sendAsset <- Asset.fromXDR(op.getSendAsset)
    destAsset <- Asset.fromXDR(op.getDestAsset)
    path <- sequence(op.getPath.map(Asset.fromXDR))
    pathPaymentOp <- Try {
      PathPaymentOperation(
        sendMax = Amount(op.getSendMax.getInt64.longValue, sendAsset),
        destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
        destinationAmount = Amount(op.getDestAmount.getInt64.longValue(), destAsset),
        path = path
      )
    }
  } yield pathPaymentOp
}

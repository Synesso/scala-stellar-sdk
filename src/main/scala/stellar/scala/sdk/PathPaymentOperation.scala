package stellar.scala.sdk

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.PATH_PAYMENT
import org.stellar.sdk.xdr._

import scala.util.{Failure, Success, Try}

case class PathPaymentOperation(sendMax: Amount,
                                destinationAccount: PublicKeyOps,
                                destinationAmount: Amount,
                                path: Seq[Asset],
                                sourceAccount: Option[KeyPair] = None) extends Operation {

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

object PathPaymentOperation {
  def apply(sourceAccount: KeyPair,
            sendMax: Amount,
            destinationAccount: PublicKeyOps,
            destinationAmount: Amount,
            path: Seq[Asset]): PathPaymentOperation = {

    PathPaymentOperation(sendMax, destinationAccount, destinationAmount, path, Some(sourceAccount))
  }

  def from(op: PathPaymentOp): Try[PathPaymentOperation] = for {
    sendAsset <- Asset.fromXDR(op.getSendAsset)
    destAsset <- Asset.fromXDR(op.getDestAsset)
    path <- sequence(op.getPath.map(Asset.fromXDR))
    pathPaymentOp <- Try {
      PathPaymentOperation(
        sendMax = Amount(op.getSendMax.getInt64.longValue, sendAsset),
        destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
        destinationAmount = Amount(op.getDestAmount.getInt64.longValue(), destAsset),
        path = path,
        sourceAccount = Option.empty[KeyPair]
      )
    }
  } yield pathPaymentOp

  private def sequence[T](tries: Seq[Try[T]]): Try[Seq[T]] = {
    tries.foldLeft(Success(Seq.empty[T]): Try[Seq[T]]) {
      case (Success(acc), Success(t)) => Success(t +: acc)
      case (Success(_), Failure(t)) => Failure(t)
      case (failure, _) => failure
    }.map(_.reverse)
  }
}

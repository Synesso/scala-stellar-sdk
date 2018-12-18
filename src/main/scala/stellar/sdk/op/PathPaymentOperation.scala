package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.PATH_PAYMENT
import org.stellar.sdk.xdr.{PublicKey => _, _}
import stellar.sdk
import stellar.sdk.TrySeq._
import stellar.sdk._

import scala.util.Try

/**
  * Represents a payment from one account to another through a path. This type of payment starts as one type of asset
  * and ends as another type of asset. There can be other assets that are traded into and out of along the path.
  * Suitable orders must exist on the relevant order books for this operation to be successful.
  *
  * @param sendMax the maximum amount willing to be spent to effect the payment
  * @param destinationAccount the payment recipient
  * @param destinationAmount the exact amount to be received
  * @param path the intermediate assets to traverse (may be empty)
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#path-payment endpoint doc]]
  */
case class PathPaymentOperation(sendMax: Amount,
                                destinationAccount: PublicKeyOps,
                                destinationAmount: Amount,
                                path: Seq[sdk.Asset] = Nil,
                                sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {

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

  override def encode: Stream[Byte] =
    Encode.int(2) ++
      sendMax.encode ++
      destinationAccount.encode ++
      destinationAmount.encode ++
      Encode.varArr(path)
}

object PathPaymentOperation {

  def from(op: PathPaymentOp, source: Option[PublicKey]): Try[PathPaymentOperation] = for {
    sendAsset <- sdk.Asset.fromXDR(op.getSendAsset)
    destAsset <- sdk.Asset.fromXDR(op.getDestAsset)
    path <- sequence(op.getPath.map(sdk.Asset.fromXDR))
    pathPaymentOp <- Try {
      PathPaymentOperation(
        sendMax = Amount(op.getSendMax.getInt64.longValue, sendAsset),
        destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
        destinationAmount = Amount(op.getDestAmount.getInt64.longValue(), destAsset),
        path = path,
        sourceAccount = source
      )
    }
  } yield {
    pathPaymentOp
  }
}

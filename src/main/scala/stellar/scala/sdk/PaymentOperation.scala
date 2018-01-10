package stellar.scala.sdk

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr._

import scala.util.Try

/**
  * Represents <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html#payment" target="_blank">Payment</a> operation.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html" target="_blank">List of Operations</a>
  */
case class PaymentOperation(destinationAccount: PublicKeyOps,
                            asset: Asset,
                            amount: Amount,
                            sourceAccount: Option[KeyPair] = None) extends Operation {

  override def toOperationBody: OperationBody = {
    val op = new CreateAccountOp()
    val destination = new AccountID()
    destination.setAccountID(destinationAccount.getXDRPublicKey)
    op.setDestination(destination)
    val startBal = new Int64()
    startBal.setInt64(amount.stroops)
    op.setStartingBalance(startBal)
    val body = new org.stellar.sdk.xdr.Operation.OperationBody()
    body.setDiscriminant(OperationType.CREATE_ACCOUNT)
    body.setCreateAccountOp(op)
    body
  }

}

object PaymentOperation {

  def apply(source: KeyPair, destination: PublicKeyOps, asset: Asset, amount: Amount): PaymentOperation = {
    PaymentOperation(destination, asset, amount, Some(source))
  }

  def from(op: PaymentOp): Try[PaymentOperation] = for {
    asset <- Asset.fromXDR(op.getAsset)
    paymentOp <- Try {
      PaymentOperation(
        sourceAccount = None,
        destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
        asset = asset,
        amount = Amount(op.getAmount.getInt64.longValue)
      )
    }
  } yield paymentOp
}

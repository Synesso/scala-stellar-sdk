package stellar.scala.sdk

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.PAYMENT
import org.stellar.sdk.xdr._

import scala.util.Try

/**
  * Represents <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html#payment" target="_blank">Payment</a> operation.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html" target="_blank">List of Operations</a>
  */
case class PaymentOperation(destinationAccount: PublicKeyOps,
                            amount: Amount,
                            sourceAccount: Option[KeyPair] = None) extends Operation {

  override def toOperationBody: OperationBody = {
    val op = new PaymentOp()
    val destination = new AccountID()
    destination.setAccountID(destinationAccount.getXDRPublicKey)
    op.setDestination(destination)
    op.setAsset(amount.asset.toXDR)
    val amnt = new Int64()
    amnt.setInt64(amount.units)
    op.setAmount(amnt)
    val body = new org.stellar.sdk.xdr.Operation.OperationBody()
    body.setDiscriminant(PAYMENT)
    body.setPaymentOp(op)
    body
  }

}

object PaymentOperation {

  def apply(source: KeyPair, destination: PublicKeyOps, amount: Amount): PaymentOperation = {
    PaymentOperation(destination, amount, Some(source))
  }

  def from(op: PaymentOp): Try[PaymentOperation] = for {
    asset <- Asset.fromXDR(op.getAsset)
    paymentOp <- Try {
      PaymentOperation(
        sourceAccount = None,
        destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
        amount = Amount(op.getAmount.getInt64.longValue, asset)
      )
    }
  } yield paymentOp
}

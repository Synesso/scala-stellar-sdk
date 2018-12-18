package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.PAYMENT
import org.stellar.sdk.xdr._
import stellar.sdk
import stellar.sdk.{Amount, Encode, KeyPair, PublicKey, PublicKeyOps}

import scala.util.Try

/**
  * Represents a payment from one account to another. This payment can be either a simple native asset payment or a
  * fiat asset payment.
  *
  * @param destinationAccount the recipient of the payment
  * @param amount the amount to be paid
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#payment endpoint doc]]
  */
case class PaymentOperation(destinationAccount: PublicKeyOps,
                            amount: Amount,
                            sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {

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

  override def encode: Stream[Byte] =
    Encode.int(1) ++
      destinationAccount.encode ++
      amount.encode

}

object PaymentOperation {
  def from(op: PaymentOp, source: Option[PublicKey]): Try[PaymentOperation] = for {
    asset <- sdk.Asset.fromXDR(op.getAsset)
    paymentOp <- Try {
      PaymentOperation(
        destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
        amount = Amount(op.getAmount.getInt64.longValue, asset),
        sourceAccount = source
      )
    }
  } yield {
    paymentOp
  }
}

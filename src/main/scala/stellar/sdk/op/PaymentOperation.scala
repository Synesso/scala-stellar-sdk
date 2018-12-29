package stellar.sdk.op

import cats.data.State
import stellar.sdk.xdr.Encode
import stellar.sdk.{Amount, KeyPair, PublicKeyOps}

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

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(1) ++
      destinationAccount.encode ++
      amount.encode

}

object PaymentOperation {

  def decode: State[Seq[Byte], PaymentOperation] = for {
    destination <- KeyPair.decode
    amount <- Amount.decode
  } yield PaymentOperation(destination, amount)

}

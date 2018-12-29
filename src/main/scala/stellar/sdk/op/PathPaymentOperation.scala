package stellar.sdk.op

import cats.data.State
import stellar.sdk
import stellar.sdk._
import stellar.sdk.xdr.{Decode, Encode}

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

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(2) ++
      sendMax.encode ++
      destinationAccount.encode ++
      destinationAmount.encode ++
      Encode.arr(path)
}

object PathPaymentOperation {

  def decode: State[Seq[Byte], PathPaymentOperation] = for {
    sendMax <- Amount.decode
    destAccount <- KeyPair.decode
    destAmount <- Amount.decode
    path <- Decode.arr(Asset.decode)
  } yield PathPaymentOperation(sendMax, destAccount, destAmount, path)

}

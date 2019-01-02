package stellar.sdk.model.op

import cats.data.State
import stellar.sdk._
import stellar.sdk.model.IssuedAmount
import stellar.sdk.model.xdr.Encode

/**
  * The source account is stating that it will trust the asset of the limit up to the amount of the limit.
  *
  * @param limit the asset to be trusted and the limit of that trust
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#change-trust endpoint doc]]
  */
case class ChangeTrustOperation(limit: IssuedAmount, sourceAccount: Option[PublicKeyOps] = None) extends Operation {
  override def encode: Stream[Byte] = super.encode ++ Encode.int(6) ++ limit.encode
}

object ChangeTrustOperation {
  def decode: State[Seq[Byte], ChangeTrustOperation] = IssuedAmount.decode.map(ChangeTrustOperation(_))
}

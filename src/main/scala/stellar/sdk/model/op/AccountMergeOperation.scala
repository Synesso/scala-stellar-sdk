package stellar.sdk.model.op

import cats.data.State
import stellar.sdk.model.xdr.Encode
import stellar.sdk.{KeyPair, PublicKeyOps}

/**
  * Deletes account and transfers remaining balance to destination account.
  *
  * @param destination the account to receive the residual balances of the account to be merged
  * @param sourceAccount the account to be merged, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#account-merge endpoint doc]]
  */
case class AccountMergeOperation(destination: PublicKeyOps, sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {
  override def encode: Stream[Byte] = super.encode ++ Encode.int(8) ++ destination.encode
}

object AccountMergeOperation {
  def decode: State[Seq[Byte], AccountMergeOperation] = KeyPair.decode.map(AccountMergeOperation(_))
}

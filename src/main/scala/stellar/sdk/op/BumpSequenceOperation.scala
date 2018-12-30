package stellar.sdk.op

import cats.data.State
import stellar.sdk.{ByteArrays, PublicKeyOps}
import stellar.sdk.xdr.{Decode, Encode}

/**
  * Bumps forward the sequence number of the source account of the operation, allowing it to invalidate any transactions
  * with a smaller sequence number.
  *
  * @param bumpTo the number to increase the sequence number to
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#bump-sequence endpoint doc]]
  */
case class BumpSequenceOperation(bumpTo: Long,
                                 sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def encode: Stream[Byte] = super.encode ++ Encode.int(11) ++ Encode.long(bumpTo)
}

object BumpSequenceOperation {
  def decode: State[Seq[Byte], BumpSequenceOperation] = Decode.long.map(BumpSequenceOperation(_))
}

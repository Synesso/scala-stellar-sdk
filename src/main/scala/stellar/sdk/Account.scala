package stellar.sdk

/**
  * Represents an account in Stellar network with its sequence number.
  */
case class Account(publicKey: PublicKeyOps, sequenceNumber: Long) extends Encodable {
  def withIncSeq: Account = this.copy(sequenceNumber = this.sequenceNumber + 1)

  override def encode: Stream[Byte] = publicKey.encode
}

package stellar.sdk

/**
  * Represents an account in Stellar network with its sequence number.
  */
case class Account(publicKey: PublicKeyOps, sequenceNumber: Long) {
  def withIncSeq: Account = this.copy(sequenceNumber = this.sequenceNumber + 1)
}


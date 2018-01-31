package stellar.sdk

/**
  * Represents an account in Stellar network with its sequence number.
  */
case class Account(keyPair: KeyPair, sequenceNumber: Long) {
  def withIncrementedSequentNumber: Account = this.copy(sequenceNumber = this.sequenceNumber + 1)
}

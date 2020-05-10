package stellar.sdk.model

/**
  * Represents an account in Stellar network with its sequence number.
  */
case class Account(id: AccountId, sequenceNumber: Long) {
  def withIncSeq: Account = this.copy(sequenceNumber = this.sequenceNumber + 1)
}

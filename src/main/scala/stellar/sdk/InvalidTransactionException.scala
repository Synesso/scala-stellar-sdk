package stellar.sdk

/**
 * Indicates that the transaction was not submittable because of an invariant check failure.
 */
case class InvalidTransactionException(message: String) extends Exception(message)

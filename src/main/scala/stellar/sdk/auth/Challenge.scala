package stellar.sdk.auth

import stellar.sdk.model.SignedTransaction

/**
 * An authentication challenge as specified in SEP-0010
 * @param signedTransaction a specially formed transaction that forms the basis of the challenge.
 * @param networkPassphrase the passphrase of the network that the transaction is (and should continue to be) signed for.
 */
case class Challenge(
  signedTransaction: SignedTransaction,
  networkPassphrase: String
) {
  def transaction = signedTransaction.transaction
}

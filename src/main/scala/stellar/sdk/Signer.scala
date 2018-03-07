package stellar.sdk

sealed trait Signer {
  val weight: Int
}

case class AccountSigner(key: PublicKeyOps, weight: Int) extends Signer

case class HashSigner(hash: String, weight: Int) extends Signer

case class PreAuthTxnSigner(hash: String, weight: Int) extends Signer


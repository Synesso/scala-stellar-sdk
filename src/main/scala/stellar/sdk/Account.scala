package stellar.sdk

import cats.data.State

/**
  * Represents an account in Stellar network with its sequence number.
  */
case class Account(publicKey: PublicKeyOps, sequenceNumber: Long) {
  def withIncSeq: Account = this.copy(sequenceNumber = this.sequenceNumber + 1)
}

object Account {
  def decode: State[Seq[Byte], Account] = for {
    pk <- KeyPair.decode
  } yield Account(pk, -1) // todo - -1???
}

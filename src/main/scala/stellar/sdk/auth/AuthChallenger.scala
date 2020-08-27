package stellar.sdk.auth

import java.security.SecureRandom
import java.time.Clock

import stellar.sdk.model.op.WriteDataOperation
import stellar.sdk.model._
import stellar.sdk.{KeyPair, Network}

import scala.concurrent.duration._

/**
 * Factory for creating authentication challenges.
 * @param serverKey This must be the keypair associated with the SIGNING_KEY as defined in your service's stellar.toml
 */
class AuthChallenger(
  serverKey: KeyPair,
  clock: Clock = Clock.systemUTC()
)(implicit network: Network) {

  /**
   * Generates a Challenge for the given account.
   * @param accountId   The stellar account that the wallet wishes to authenticate with the server
   * @param homeDomain  The fully qualified domain name of the service requiring authentication.
   * @param timeout     The period that during which valid responses to this challenge will be accepted.
   */
  def challenge(
    accountId: AccountId,
    homeDomain: String,
    timeout: Duration = 15.minutes
  ): Challenge = Challenge(
    Transaction(
      source = Account(serverKey.toAccountId, 0L),
      operations = List(
        WriteDataOperation(
          name = s"$homeDomain auth",
          value = generateDataKey,
          sourceAccount = Some(accountId.publicKey)
        )
      ),
      timeBounds = TimeBounds.timeout(timeout, clock),
      maxFee = NativeAmount(100)
    ).sign(serverKey), network.passphrase,
    clock
  )

  private def generateDataKey: Array[Byte] = {
    val bs = Array.ofDim[Byte](48)
    SecureRandom.getInstanceStrong.nextBytes(bs)
    bs
  }
}

package stellar.sdk.auth

import java.security.SecureRandom

import stellar.sdk.model.op.WriteDataOperation
import stellar.sdk.model._
import stellar.sdk.{KeyPair, Network}

import scala.concurrent.duration._

class AuthChallenger(
  serverKey: KeyPair,
  implicit val network: Network
) {

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
      timeBounds = TimeBounds.timeout(timeout),
      maxFee = NativeAmount(100)
    ).sign(serverKey), network.passphrase
  )

  private def generateDataKey: Array[Byte] = {
    val bs = Array.ofDim[Byte](48)
    SecureRandom.getInstanceStrong.nextBytes(bs)
    bs
  }
}

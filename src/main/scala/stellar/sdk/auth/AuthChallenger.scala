package stellar.sdk.auth

import java.security.SecureRandom
import java.time.Clock
import com.google.common.base.Charsets
import stellar.sdk.model.op.WriteDataOperation
import stellar.sdk.model.{domain, _}
import stellar.sdk.model.domain.DomainInfo
import stellar.sdk.{KeyPair, Network}

import scala.concurrent.{ExecutionContext, Future}
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
   *
   * @param accountId     The stellar account that the wallet wishes to authenticate with the server
   * @param homeDomain    The fully qualified domain name of the stellar.toml file describing the service requiring
   *                      authentication.
   * @param webAuthDomain The fully qualified domain name of the service requiring authentication. This may or may not
   *                      be the same as `homeDomain`.
   * @param clientDomain  The fully qualified domain name of the client service requesting authentication. If present,
   *                      the challenge will include a request that the client attach a signature from the associated
   *                      domain accounts signing key.
   * @param timeout       The period that during which valid responses to this challenge will be accepted.
   */
  def challenge(
    accountId: AccountId,
    homeDomain: String,
    webAuthDomain: String,
    clientDomain: Option[String] = None,
    timeout: Duration = 15.minutes
    // TODO - I'm not convinced it is correct to make this an IO. Pass through the client account id instead.
  )(implicit ec: ExecutionContext): Future[Challenge] = {

    val clientDomainWriteDataOperation: Future[Option[WriteDataOperation]] = clientDomain.map(domain =>
      DomainInfo.forDomain(domain).map(_.flatMap(_.signingKey.map(key =>
        WriteDataOperation(
          name = "client_domain",
          value = domain.getBytes(Charsets.UTF_8),
          sourceAccount = Some(key.toAccountId)
        )
      )))
    ).getOrElse(Future.successful(None))

    clientDomainWriteDataOperation.map { it =>
      Challenge(
        Transaction(
          source = Account(serverKey.toAccountId, 0L),
          operations = List(
            Some(WriteDataOperation(
              name = s"$homeDomain auth",
              value = generateDataKey,
              sourceAccount = Some(accountId)
            )),
            Some(WriteDataOperation(
              name = "web_auth_domain",
              value = webAuthDomain.getBytes(Charsets.UTF_8),
              sourceAccount = Some(serverKey.toAccountId)
            )),
            it
          ).flatten,
          timeBounds = TimeBounds.timeout(timeout, clock),
          maxFee = NativeAmount(200)
        ).sign(serverKey), network.passphrase,
        clock
      )
    }
  }

  /**
   * Generates a Challenge for the given account.
   *
   * This is a convenience method where the webAuthDomain equals the homeDomain.
   *
   * @param accountId     The stellar account that the wallet wishes to authenticate with the server
   * @param homeDomain    The fully qualified domain name of the stellar.toml file describing the service requiring
   *                      authentication.
   * @param timeout       The period that during which valid responses to this challenge will be accepted.
   */
  def challenge(
    accountId: AccountId,
    homeDomain: String,
    timeout: Duration
  )(implicit ec: ExecutionContext): Future[Challenge] = challenge(accountId, homeDomain, homeDomain, None, timeout)

  /**
   * Generates a Challenge for the given account.
   *
   * This is a convenience method where the webAuthDomain equals the homeDomain; and the timeout is 15 minutes.
   *
   * @param accountId     The stellar account that the wallet wishes to authenticate with the server
   * @param homeDomain    The fully qualified domain name of the stellar.toml file describing the service requiring
   *                      authentication.
   */
  def challenge(
    accountId: AccountId,
    homeDomain: String
  )(implicit ec: ExecutionContext): Future[Challenge] = challenge(accountId, homeDomain, homeDomain)

  private def generateDataKey: Array[Byte] = {
    val bs = Array.ofDim[Byte](48)
    SecureRandom.getInstanceStrong.nextBytes(bs)
    bs
  }
}

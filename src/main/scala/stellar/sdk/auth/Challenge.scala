package stellar.sdk.auth

import java.time.Clock

import okio.ByteString
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import stellar.sdk.model.response.AccountResponse
import stellar.sdk.model.{Account, SignedTransaction}
import stellar.sdk.util.DoNothingNetwork
import stellar.sdk.{Network, PublicNetwork, Signature}

/**
 * An authentication challenge as specified in SEP-0010
 * @param signedTransaction a specially formed transaction that forms the basis of the challenge.
 * @param networkPassphrase the passphrase of the network that the transaction is (and should continue to be) signed for.
 */
case class Challenge(
  signedTransaction: SignedTransaction,
  networkPassphrase: String
) {

  /**
   * Verifies that the provided signed transaction is the same as the challenge and has been signed by the
   * challenged account.
   *
   * @param challenged the client account that was challenged.
   * @param answer     the transaction that may have been signed by the challenged account.
   * @param clock      the clock to used to detect timebound expiry.
   * @param network    the network that the transaction is signed for.
   */
  def verify(
    challenged: AccountResponse,
    answer: SignedTransaction,
    clock: Clock = Clock.systemUTC()
  )(implicit network: Network): ChallengeResult = {
    val sameSignatures = byteStrings(answer.signatures).containsSlice(byteStrings(signedTransaction.signatures))
    if (!sameSignatures) ChallengeMalformed("Response did not contain the challenge signatures")
    else if (!transaction.timeBounds.includes(clock.instant())) ChallengeExpired
    else if (!answer.verify(transaction.operations.head.sourceAccount.get)) ChallengeNotSignedByClient
    else ChallengeSuccess
  }

  private def byteStrings(signatures: Seq[Signature]): Seq[ByteString] =
    signatures.map(_.data).map(new ByteString(_))

  /**
   * The inner, raw transaction.
   */
  def transaction = signedTransaction.transaction

  /**
   * Encode this challenge as JSON.
   */
  def toJson: String = {
    compact(render(
      ("transaction" -> signedTransaction.encodeXDR) ~
        ("network_passphrase" -> signedTransaction.transaction.network.passphrase)
    ))
  }
}

object Challenge {

  implicit val formats: Formats = DefaultFormats

  /**
   * Decode a Challenge from JSON.
   */
  def apply(json: String): Challenge = {
    val o = parse(json)
    implicit val network = (o \ "network_passphrase").extractOpt[String]
      .map(p => new DoNothingNetwork(p))
        .getOrElse(PublicNetwork)
    Challenge(
      signedTransaction = SignedTransaction.decodeXDR((o \ "transaction").extract[String]),
      networkPassphrase = network.passphrase
    )
  }
}

/** The result of verifying a challenge. */
sealed trait ChallengeResult
case object ChallengeSuccess extends ChallengeResult
case class ChallengeMalformed(message: String) extends ChallengeResult
case object ChallengeExpired extends ChallengeResult
case object ChallengeNotSignedByClient extends ChallengeResult

/** The threshold that a cumulative weight of signatures met. */
sealed trait Threshold
case object Not
case object Low
case object Medium
case object High

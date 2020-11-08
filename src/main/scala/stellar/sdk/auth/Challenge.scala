package stellar.sdk.auth

import java.time.Clock

import okio.ByteString
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import stellar.sdk.model.response.AccountResponse
import stellar.sdk.model.{Account, AccountId, SignedTransaction, Transaction}
import stellar.sdk.util.DoNothingNetwork
import stellar.sdk.{Network, PublicNetwork, Signature}

/**
 * An authentication challenge as specified in SEP-0010
 * @param signedTransaction a specially formed transaction that forms the basis of the challenge.
 * @param networkPassphrase the passphrase of the network that the transaction is (and should continue to be) signed for.
 * @param clock             the clock to used to detect timebound expiry.
 */
case class Challenge(
  signedTransaction: SignedTransaction,
  networkPassphrase: String,
  clock: Clock = Clock.systemUTC()
) {

  /**
   * Verifies that the provided signed transaction is the same as the challenge and has been signed by the
   * challenged account.
   *
   * @param answer     the transaction that may have been signed by the challenged account.
   * @param network    the network that the transaction is signed for.
   */
  def verify(answer: SignedTransaction)(implicit network: Network): ChallengeResult =
    checkSequenceNumber(answer)
      .orElse(checkExpiry(transaction))
      .orElse(checkSameSignatures(signedTransaction, answer))
      .orElse(checkSignedByClient(answer))
      .getOrElse(ChallengeSuccess)

  /**
   * Verifies that the provided signed transaction is the same as the challenge and has been signed by the
   * enough of the signers to meet the required threshold.
   *
   * @param answer     the transaction that may have been signed by the challenged account.
   * @param challenged the client account that was challenged.
   * @param threshold  the required threshold to be met by the signers.
   * @param network    the network that the transaction is signed for.
   */
  def verify(
    answer: SignedTransaction,
    challenged: AccountResponse,
    threshold: Threshold
  )(implicit network: Network): ChallengeResult =
    checkSequenceNumber(answer)
      .orElse(checkExpiry(transaction))
      .orElse(checkSameSignatures(signedTransaction, answer))
      .orElse(checkSignaturesMatchThreshold(answer, challenged, threshold))
      .getOrElse(ChallengeSuccess)

  private def checkSequenceNumber(answer: SignedTransaction): Option[ChallengeResult] =
    if (answer.transaction.source.sequenceNumber == 0) None
    else Some(ChallengeMalformed("Transaction did not have a sequenceNumber of zero"))

  private def checkSameSignatures(original: SignedTransaction, answer: SignedTransaction): Option[ChallengeResult] = {
    val challengeSigs = byteStrings(original.signatures.toSet)
    val answerSigs = byteStrings(answer.signatures.toSet)
    if (challengeSigs.intersect(answerSigs) != challengeSigs)
      Some(ChallengeMalformed("Response did not contain the challenge signatures"))
    else None
  }

  private def checkExpiry(transaction: Transaction): Option[ChallengeResult] =
    if (transaction.timeBounds.includes(clock.instant())) None
    else Some(ChallengeExpired)

  private def checkSignedByClient(answer: SignedTransaction): Option[ChallengeResult] = {
    // FIXME - deal with 0 or multi operations. Deal with no source account.
    if (answer.verify(transaction.operations.head.sourceAccount.get)) None
    else Some(ChallengeNotSignedByClient)
  }

  private def checkSignaturesMatchThreshold(
    answer: SignedTransaction,
    challenged: AccountResponse,
    threshold: Threshold
  ): Option[ChallengeResult] = {
    val cumulativeWeight = challenged.signers.flatMap(signer => signer.key match {
      case a: AccountId => Some(a -> signer.weight)
      case _ => Option.empty[(AccountId, Int)]
    }).flatMap { case (accountId, weight) => if (answer.verify(accountId.publicKey)) Some(weight) else None }
      .sum
    val attained =
      if (cumulativeWeight >= challenged.thresholds.high) Some(High)
      else if (cumulativeWeight >= challenged.thresholds.med) Some(Medium)
      else if (cumulativeWeight >= challenged.thresholds.low) Some(Low)
      else None

    val tooLow = attained.forall(_ < threshold)
    if (tooLow) Some(ChallengeThresholdNotMet(threshold, attained))
    else None
  }


  private def byteStrings(signatures: Set[Signature]): Set[ByteString] =
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
    implicit val network: Network = (o \ "network_passphrase").extractOpt[String]
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
case class ChallengeThresholdNotMet(expected: Threshold, attained: Option[Threshold]) extends ChallengeResult

/** The threshold that a cumulative weight of signatures met. */
sealed trait Threshold {
  def <(o: Threshold): Boolean
}
case object Low extends Threshold {
  override def <(o: Threshold): Boolean = o != Low
}
case object Medium extends Threshold {
  override def <(o: Threshold): Boolean = o == High
}
case object High extends Threshold {
  override def <(o: Threshold): Boolean = false
}

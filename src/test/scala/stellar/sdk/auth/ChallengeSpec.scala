package stellar.sdk.auth

import java.time.Instant
import com.google.common.base.Charsets
import monocle.macros.GenLens
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.op.{Operation, WriteDataOperation}
import stellar.sdk.model.response.AccountResponse
import stellar.sdk.model.{Signer, Thresholds}
import stellar.sdk.util.FakeClock
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair, PublicNetwork}

import scala.concurrent.duration._

/**
 * Describes the implementation of behaviour specified by SEP-10.
 *
 * @see https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0010.md
 */
class ChallengeSpec(implicit ee: ExecutionEnv) extends Specification with DomainMatchers with ArbitraryInput {

  private val serverKey = KeyPair.random

  private def fixtures: (AuthChallenger, FakeClock, KeyPair, AccountResponse, List[KeyPair]) = {
    val fakeClock = FakeClock()
    val signersWithWeights = List(1, 2, 3).map { w =>
      val key = KeyPair.random
      key -> Signer(key.toAccountId, w)
    }
    val clientKey = KeyPair.random
    val challenged = AccountResponse(clientKey.asPublicKey, 0, 0, Thresholds(2, 4, 6),
      authRequired = false, authRevocable = false, Nil, signersWithWeights.map(_._2), None, 0, 0, Map.empty)
    val challenger = new AuthChallenger(serverKey, fakeClock)(PublicNetwork)
    (challenger, fakeClock, clientKey, challenged, signersWithWeights.map(_._1))
  }

  "a generated authentication challenge" should {
    "have the source account set the the server's signing account" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.source.id mustEqual serverKey.toAccountId
    }

    "have a sequence number of zero" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.source.sequenceNumber mustEqual 0
    }

    "default to a timeout of the recommended 15 minutes" >> {
      val (subject, clock, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.timeBounds.end must beLike { when: Instant =>
        when.toEpochMilli mustEqual clock.instant().plusSeconds(900).toEpochMilli
      }
    }

    "include the actual timeout if one is specified" >> {
      val (subject, clock, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com", timeout = 3.minutes)
      challenge.transaction.timeBounds.end mustEqual clock.instant().plusSeconds(180)
    }

    "have a two manage data operations from the challenged account" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.size mustEqual 2
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.name mustEqual "test.com auth"
        op.sourceAccount must beSome(clientKey.asPublicKey.toAccountId)
      }
      challenge.transaction.operations(1) must beLike[Operation] { case op: WriteDataOperation =>
        op.name mustEqual "web_auth_domain"
        op.value mustEqual "test.com".getBytes(Charsets.UTF_8).toSeq
        op.sourceAccount must beSome(clientKey.asPublicKey.toAccountId)
      }
    }

    "allow distinct home and web-auth domains" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "red.com", "green.com")
      challenge.transaction.operations.size mustEqual 2
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.name mustEqual "red.com auth"
        op.sourceAccount must beSome(clientKey.asPublicKey.toAccountId)
      }
      challenge.transaction.operations(1) must beLike[Operation] { case op: WriteDataOperation =>
        op.name mustEqual "web_auth_domain"
        op.value mustEqual "green.com".getBytes(Charsets.UTF_8).toSeq
        op.sourceAccount must beSome(clientKey.asPublicKey.toAccountId)
      }
    }

    "be signed by the provided server key" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.challengeTransaction.signatures.size mustEqual 1
      challenge.challengeTransaction.verify(serverKey) must beTrue
    }

    "json serialise and deserialise" >> prop { clientKey: KeyPair =>
      val (authChallenger, _, clientKey, _, _) = fixtures
      // #challenge_to_from_json_example
      val challenge = authChallenger.challenge(clientKey.toAccountId, "test.com")
      Challenge(challenge.toJson) must beEquivalentTo(challenge)
      // #challenge_to_from_json_example
    }
  }

  "the operation in the generated authentication challenge" should {
    "have the key '<home domain> auth'" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.name mustEqual "test.com auth"
      }
    }

    "disallow home domains greater than 59 characters" >> {
      val (subject, _, clientKey, _, _) = fixtures
      subject.challenge(
        clientKey.toAccountId,
        homeDomain = "." * 60
      ) should throwAn[IllegalArgumentException]
    }

    "have a cryptographic random 48 byte value" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.value must haveSize(48)
      }
    }
  }

  "verifying a challenge response signed by master key" should {
    "succeed using signed transaction when the challenge is correctly signed" >> {
      val (authChallenger, _, clientKey, _, _) = fixtures
      // #auth_challenge_success_example
      val challenge = authChallenger.challenge(
        accountId = clientKey.toAccountId,
        homeDomain = "test.com",
        timeout = 15.minutes
      )
      val answer = challenge.challengeTransaction.sign(clientKey)
      challenge.verify(answer) mustEqual ChallengeSuccess
      // #auth_challenge_success_example
    }

    "fail if the source account does not match that of the challenge" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = challenge.copy(challengeTransaction = challenge.challengeTransaction.sign(KeyPair.random)).challengeTransaction
      challenge.verify(answer) mustEqual ChallengeNotSignedByClient
    }

    "fail if the signed transaction does not contain the signatures of the challenge" >> {
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = challenge.challengeTransaction.copy(signatures = Nil).sign(clientKey)
      challenge.verify(answer) must beEqualTo(
        ChallengeMalformed("Response did not contain the challenge signatures")
      )
    }

    "fail if the timebounds are expired" >> {
      val (subject, clock, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
        .copy(clock = clock)
      val answer = challenge.challengeTransaction.sign(clientKey)
      clock.advance(16.minutes)
      challenge.verify(answer) mustEqual ChallengeExpired
    }

    "fail if the transaction sequence is not zero" >> {
      val seqNumberLens = GenLens[Challenge](_.challengeTransaction.transaction.source.sequenceNumber)
      val (subject, _, clientKey, _, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = seqNumberLens.modify(_ => 1L)(challenge).challengeTransaction.sign(clientKey)
      challenge.verify(answer) must beEqualTo(
        ChallengeMalformed("Transaction did not have a sequenceNumber of zero")
      )
    }
  }

  "verifying a challenge response signed by different signers" should {
    "succeed when valid for the account" >> {
      val (subject, _, clientKey, challenged, signers) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = signers.foldLeft(challenge.challengeTransaction)(_ sign _)
      challenge.verify(answer, challenged, Low) mustEqual ChallengeSuccess
    }

    "fail when desired threshold not met" >> {
      val (subject, _, clientKey, challenged, signers) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = signers.tail.foldLeft(challenge.challengeTransaction)(_ sign _)
      challenge.verify(answer, challenged, High) mustEqual ChallengeThresholdNotMet(High, Some(Medium))
    }

    "fail when no threshold met" >> {
      val (subject, _, clientKey, challenged, signers) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = challenge.challengeTransaction.sign(signers.head)
      challenge.verify(answer, challenged, Low) mustEqual ChallengeThresholdNotMet(Low, None)
    }

    "fail when there are no signers" >> {
      val (subject, _, clientKey, challenged, _) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = challenge.challengeTransaction
      challenge.verify(answer, challenged, Low) mustEqual ChallengeThresholdNotMet(Low, None)
    }
  }
}

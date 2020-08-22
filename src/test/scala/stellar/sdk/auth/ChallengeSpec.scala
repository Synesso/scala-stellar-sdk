package stellar.sdk.auth

import java.time.Instant

import org.specs2.mutable.Specification
import stellar.sdk.model.op.{Operation, WriteDataOperation}
import stellar.sdk.util.FakeClock
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair, PublicNetwork}

import scala.concurrent.duration._

/**
 * Describes the implementation of behaviour specified by SEP-10.
 * @see https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0010.md
 */
class ChallengeSpec extends Specification with DomainMatchers with ArbitraryInput {

  private val serverKey = KeyPair.random
  private def fixtures: (AuthChallenger, FakeClock, KeyPair) = {
    val fakeClock = FakeClock()
    (new AuthChallenger(serverKey, fakeClock)(PublicNetwork), fakeClock, KeyPair.random)
  }

  "a generated authentication challenge" should {
    "have the source account set the the server's signing account" >> {
      val (subject, _, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.source.id mustEqual serverKey.toAccountId
    }

    "have a sequence number of zero" >> {
      val (subject, _, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.source.sequenceNumber mustEqual 0
    }

    "default to a timeout of the recommended 15 minutes" >> {
      val (subject, clock, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.timeBounds.end must beLike { when: Instant =>
        when.toEpochMilli mustEqual clock.instant().plusSeconds(900).toEpochMilli
      }
    }

    "include the actual timeout if one is specified" >> {
      val (subject, clock, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com", timeout = 3.minutes)
      challenge.transaction.timeBounds.end mustEqual clock.instant().plusSeconds(180)
    }

    "have a single manage data operation from the challenged account" >> {
      val (subject, _, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.size mustEqual 1
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.sourceAccount must beSome(clientKey.asPublicKey)
      }
    }

    "be signed by the provided server key" >> {
      val (subject, _, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.signedTransaction.signatures.size mustEqual 1
      challenge.signedTransaction.verify(serverKey) must beTrue
    }

    "json serialise and deserialise" >> prop { clientKey: KeyPair =>
      val (subject, _, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      Challenge(challenge.toJson) must beEquivalentTo(challenge)
    }
  }

  "the operation in the generated authentication challenge" should {
    "have the key '<home domain> auth'" >> {
      val (subject, _, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.name mustEqual "test.com auth"
      }
    }

    "disallow home domains greater than 59 characters" >> {
      val (subject, _, clientKey) = fixtures
      subject.challenge(
        clientKey.toAccountId,
        homeDomain = "." * 60
      ) should throwAn[IllegalArgumentException]
    }

    "have a cryptographic random 48 byte value" >> {
      val (subject, _, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.value must haveSize(48)
      }
    }
  }

  "verifying a challenge response" should {
    "succeed using signed transaction when the challenge is correctly signed" >> {
      val (subject, clock, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = challenge.signedTransaction.sign(clientKey)
      challenge.verify(answer, clock) mustEqual ChallengeSuccess
    }

    "fail if the source account does not match that of the challenge" >> {
      val (subject, clock, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = challenge.copy(signedTransaction = challenge.signedTransaction.sign(KeyPair.random)).signedTransaction
      challenge.verify(answer, clock) mustEqual ChallengeNotSignedByClient
    }

    "fail if the signed transaction does not contain the signatures of the challenge" >> {
      val (subject, clock, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = challenge.signedTransaction.copy(signatures = Nil).sign(clientKey)
      challenge.verify(answer, clock) must beEqualTo(
        ChallengeMalformed("Response did not contain the challenge signatures")
      )
    }

    "fail if the timebounds are expired" >> {
      val (subject, clock, clientKey) = fixtures
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      val answer = challenge.signedTransaction.sign(clientKey)
      clock.advance(16.minutes)
      challenge.verify(answer, clock) mustEqual ChallengeExpired
    }
  }
}

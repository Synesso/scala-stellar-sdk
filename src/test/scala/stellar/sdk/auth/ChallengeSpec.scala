package stellar.sdk.auth

import java.time.Instant

import org.specs2.mutable.Specification
import stellar.sdk.model.op.{Operation, WriteDataOperation}
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair, PublicNetwork}

import scala.concurrent.duration._

/**
 * Describes the implementation of behaviour specified by SEP-10.
 * @see https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0010.md
 */
class ChallengeSpec extends Specification with DomainMatchers with ArbitraryInput {

  val serverKey = KeyPair.random
  val subject = new AuthChallenger(serverKey, PublicNetwork)

  "a generated authentication challenge" should {
    "have the source account set the the server's signing account" >> {
      val clientKey = KeyPair.random
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.source.id mustEqual serverKey.toAccountId
    }

    "have a sequence number of zero" >> {
      val clientKey = KeyPair.random
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.source.sequenceNumber mustEqual 0
    }

    "default to a timeout of the recommended 15 minutes" >> {
      val clientKey = KeyPair.random
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.timeBounds.end must beLike { when: Instant =>
        when.toEpochMilli must beCloseTo(Instant.now().plusSeconds(900).toEpochMilli, 5_000)
      }
    }

    "include the actual timeout if one is specified" >> {
      val clientKey = KeyPair.random
      val challenge = subject.challenge(clientKey.toAccountId, "test.com", timeout = 3.minutes)
      challenge.transaction.timeBounds.end must beLike { when: Instant =>
        when.toEpochMilli must beCloseTo(Instant.now().plusSeconds(180).toEpochMilli, 2_000)
      }
    }

    "have a single manage data operation from the challenged account" >> {
      val clientKey = KeyPair.random
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.size mustEqual 1
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.sourceAccount must beSome(clientKey.asPublicKey)
      }
    }

    "be signed by the provided server key" >> {
      val clientKey = KeyPair.random
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.signedTransaction.signatures.size mustEqual 1
      challenge.signedTransaction.verify(serverKey) must beTrue
    }

    "json serialise and deserialise" >> prop { clientKey: KeyPair =>
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      Challenge(challenge.toJson) must beEquivalentTo(challenge)
    }
  }

  "the operation in the generated authentication challenge" should {
    "have the key '<home domain> auth'" >> {
      val clientKey = KeyPair.random
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.name mustEqual "test.com auth"
      }
    }

    "disallow home domains greater than 59 characters" >> {
      val clientKey = KeyPair.random
      subject.challenge(
        clientKey.toAccountId,
        homeDomain = "." * 60
      ) should throwAn[IllegalArgumentException]
    }

    "have a cryptographic random 48 byte value" >> {
      val clientKey = KeyPair.random
      val challenge = subject.challenge(clientKey.toAccountId, "test.com")
      challenge.transaction.operations.head must beLike[Operation] { case op: WriteDataOperation =>
        op.value must haveSize(48)
      }
    }
  }
}

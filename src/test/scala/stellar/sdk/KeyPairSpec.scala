package stellar.sdk

import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.apache.commons.codec.binary.Hex
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import stellar.sdk.util.{ByteArrays, WordList}

import scala.concurrent.Future
import scala.concurrent.duration._

class KeyPairSpec(implicit ee: ExecutionEnv) extends Specification with ArbitraryInput with DomainMatchers {

  private val keyPair = KeyPair.fromSecretSeed(
    Hex.decodeHex("1123740522f11bfef6b3671f51e159ccf589ccf8965262dd5f97d1721d383dd4")
  )
  private val sig = "587d4b472eeef7d07aafcd0b049640b0bb3f39784118c2e2b73a04fa2f64c9c538b4b2d0f5335e968a480021fdc23e98c0ddf424cb15d8131df8cb6c4bb58309"

  "signed data" should {
    "be verified by the signing key" >> prop { msg: String =>
      keyPair.verify(msg.getBytes("UTF-8"), keyPair.sign(msg.getBytes("UTF-8")).data) must beTrue
    }

    "be correct for concrete example" >> {
      val data = "hello world"
      val actual = keyPair.sign(data.getBytes("UTF-8")).data
      Hex.encodeHex(actual).mkString mustEqual sig
    }

    "verify true for a concrete example of a valid signature" >> {
      val data = "hello world"
      keyPair.verify(data.getBytes("UTF-8"), Hex.decodeHex(sig)) must beTrue
    }

    "verify false for a concrete example of an invalid signature" >> {
      val data = "今日は世界"
      keyPair.verify(data.getBytes("UTF-8"), Hex.decodeHex(sig)) must beFalse
    }

    "verify false for random rubbish" >> prop { msg: String =>
      keyPair.verify(msg.getBytes("UTF-8"), msg.getBytes("UTF-8")) must beFalse
    }
  }

  "a key pair" should {
    "report its account id and secret seed and be reconstituted from these" >> prop { kp: KeyPair =>
      kp.accountId.toCharArray must haveLength(56)
      kp.accountId must startWith("G")
      KeyPair.fromPublicKey(kp.publicKey) must beEquivalentTo(kp.asPublicKey)
      KeyPair.fromSecretSeed(kp.secretSeed) must beEquivalentTo(kp)
      KeyPair.fromSecretSeed(kp.secretSeed.mkString) must beEquivalentTo(kp)
      KeyPair.fromAccountId(kp.accountId) must beEquivalentTo(kp.asPublicKey)
    }

    "not be constructed from an invalid account id" >> {
      val badId = "GACZHAQLFECAHDSFDQPCOAD6ITVWR7BUZAIRRUGOAPLECX74O6223A4G"
      KeyPair.fromAccountId(badId) must throwA[InvalidAccountId].like {
        case e: InvalidAccountId => e.getMessage mustEqual badId
      }
    }

    "not be constructed from an invalid secret seed" >> {
      val nickCave = ""
      KeyPair.fromSecretSeed(nickCave) must throwAn[InvalidSecretSeed]
    }

    "have a mnemonic" >> prop { (kp: KeyPair, wordList: WordList) =>
      val phrase = kp.mnemonic(wordList)
      phrase must haveSize(24)
      phrase must contain(not(beEmpty[String])).forall
    }
  }

  "a public key" should {
    "have a hashcode equal to the account id" >> prop { pk: PublicKey =>
      pk.hashCode mustEqual pk.accountId.hashCode
    }

    "be equal to the keypair it originated from" >> prop { kp: KeyPair =>
      kp.asPublicKey mustEqual kp
    }

    "not be equal to non-PublicKeyOps instances" >> prop { pk: PublicKey =>
      pk must not(beEqualTo(pk.accountId))
    }

    "be constructable from the internal 'a-byte'" >> prop { pk: PublicKey =>
      pk must beEqualTo(KeyPair.fromPublicKey(pk.publicKey))
    }

    "be constructable from a passphrase" >> {
      // #keypair_from_passphrase
      val kp = KeyPair.fromPassphrase(
        "But, the Babel fish is a dead giveaway isn't it?"
      )
      // #keypair_from_passphrase
      kp mustEqual KeyPair.fromSecretSeed("SDHJNFV6MEPGT2FTAADH2ACHHXIV72F4VV4Q3WLYOKZTK7XB62NAOZPA")
    }

    "serde via xdr bytes" >> prop { pk: PublicKey =>
      val (remaining, decoded) = KeyPair.decode.run(pk.encode).value
      decoded must beEquivalentTo(pk)
      remaining must beEmpty
    }

    "serde via xdr string" >> prop { pk: PublicKey =>
      KeyPair.decodeXDR(ByteArrays.base64(pk.encode)) must beEquivalentTo(pk)
    }
  }

  "a federated address" should {
    "resolved to a keypair when it exists" >> {
      // #keypair_from_federated_address
      val resolved: Future[PublicKey] = KeyPair.fromAddress("jem*keybase.io")
      // #keypair_from_federated_address
      resolved must beEqualTo(
        KeyPair.fromAccountId("GBRAZP7U3SPHZ2FWOJLHPBO3XABZLKHNF6V5PUIJEEK6JEBKGXWD2IIE")
      ).awaitFor(1.minute)
    }

    "fail when the name does not exist" >> {
      KeyPair.fromAddress("asodifuawehksdjhlsduyfasdjfh*stronghold.co") must throwA[NoSuchAddress].awaitFor(1.minute)
    }

    "fail when the well-known.toml doesn't contain a federation server" >> {
      val server = new MockWebServer()
      server.enqueue(new MockResponse().setBody("FOO=123"))
      server.start()
      val response = KeyPair.fromAddress("abc*localhost:8002") must throwA[NoSuchAddress].awaitFor(1.minute)
      server.shutdown()
      response
    }

    "fail when the domain does not resolve" >> {
      KeyPair.fromAddress("jem*no.such.top.level.domain") must throwA[NoSuchAddress].awaitFor(1.minute)
    }

    "fail when the address is not in the correct format" >> {
      KeyPair.fromAddress("no asterisk") must throwA[NoSuchAddress].awaitFor(1.minute)
    }
  }
}

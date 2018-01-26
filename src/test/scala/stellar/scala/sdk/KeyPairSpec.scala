package stellar.scala.sdk

import org.apache.commons.codec.binary.Hex
import org.specs2.mutable.Specification

class KeyPairSpec extends Specification with ArbitraryInput with DomainMatchers {

  private val keyPair = KeyPair.fromSecretSeed(
    Hex.decodeHex("1123740522f11bfef6b3671f51e159ccf589ccf8965262dd5f97d1721d383dd4")
  )
  private val sig = "587d4b472eeef7d07aafcd0b049640b0bb3f39784118c2e2b73a04fa2f64c9c538b4b2d0f5335e968a480021fdc23e98c0ddf424cb15d8131df8cb6c4bb58309"

  "signed data" should {
    "be verified by the signing key" >> prop { msg: String =>
      keyPair.verify(msg.getBytes, keyPair.sign(msg.getBytes).get) must beTrue
    }

    "be correct for concrete example" >> {
      val data = "hello world"
      val actual = keyPair.sign(data.getBytes).get
      Hex.encodeHex(actual).mkString mustEqual sig
    }

    "verify true for a concrete example of a valid signature" >> {
      val data = "hello world"
      keyPair.verify(data.getBytes, Hex.decodeHex(sig)) must beTrue
    }

    "verify false for a concrete example of an invalid signature" >> {
      val data = "今日は世界"
      keyPair.verify(data.getBytes, Hex.decodeHex(sig)) must beFalse
    }

    "verify false for random rubbish" >> prop { msg: String =>
      keyPair.verify(msg.getBytes, msg.getBytes) must beFalse
    }
  }

  "a key pair" should {
    "report its account id and secret seed and be reconstituted from these" >> prop { kp: KeyPair =>
      kp.accountId.toCharArray must haveLength(56)
      kp.accountId must startWith("G")
      KeyPair.fromPublicKey(kp.publicKey) must beEquivalentTo(kp.asVerifyingKey)
      KeyPair.fromSecretSeed(kp.secretSeed) must beEquivalentTo(kp)
      KeyPair.fromSecretSeed(kp.secretSeed.mkString) must beEquivalentTo(kp)
      KeyPair.fromAccountId(kp.accountId) must beEquivalentTo(kp.asVerifyingKey)
    }
  }

}

package stellar.sdk.key

import java.nio.charset.StandardCharsets.UTF_8

import okio.ByteString
import org.scalacheck.{Arbitrary, Gen, Shrink}
import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair}

class MnemonicSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a mnemonic" should {
    "convert to entropy" in {
      Mnemonic(
        phrase = List("abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
          "abandon", "abandon", "abandon", "abandon", "about")
      ).entropy.hex() mustEqual "00000000000000000000000000000000"

      Mnemonic(
        phrase = List("legal", "winner", "thank", "year", "wave", "sausage", "worth", "useful",
          "legal", "winner", "thank", "yellow")
      ).entropy.hex() mustEqual "7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f"

      Mnemonic(
        phrase = List("letter", "advice", "cage", "absurd", "amount", "doctor", "acoustic", "avoid",
          "letter", "advice", "cage", "above")
      ).entropy.hex() mustEqual "80808080808080808080808080808080"

      Mnemonic(
        phrase = List("all", "hour", "make", "first", "leader", "extend", "hole", "alien", "behind",
          "guard", "gospel", "lava", "path", "output", "census", "museum", "junior", "mass",
          "reopen", "famous", "sing", "advance", "salt", "reform")
      ).entropy.hex() mustEqual "066dca1a2bb7e8a1db2832148ce9933eea0f3ac9548d793112d9a95c9407efad"
    }

    "be generated randomly and form keypairs from seed" >> prop { (wordList: WordList, entropyBits: Int) =>
      val mnemonic = Mnemonic.random(wordList, entropyBits)
      KeyPair.fromMnemonic(mnemonic, wordList = wordList) must beEquivalentTo(
        KeyPair.fromSecretSeed(mnemonic.asHDNode().deriveChild(44, 148, 0).privateKey))
    }.setArbitraries(
      arbWordList,
      Arbitrary(Gen.oneOf(128, 160, 192, 224, 256))
    ).setShrink2(Shrink(_ => Stream.empty[Int]))

    "be generated randomly and form valid keypairs" >> prop { (wordList: WordList, entropyBits: Int) =>
      val mnemonic = Mnemonic.random(wordList, entropyBits)
      mnemonic.asRootKeyPair() must beEquivalentTo(
        KeyPair.fromMnemonic(mnemonic, wordList = wordList)
      )
    }.setArbitraries(
      arbWordList,
      Arbitrary(Gen.oneOf(128, 160, 192, 224, 256))
    ).setShrink2(Shrink(_ => Stream.empty[Int]))

    "an example of constructing a random mnemonic and a keypair from it" >> {
      // #mnemonic-random-spanish
      val mnemonic = Mnemonic.random(SpanishWords, entropyBits = 128)
      val passphrase = new ByteString("perro salchicha".getBytes(UTF_8))
      val keyPair = mnemonic.asRootKeyPair(passphrase)
      KeyPair.fromMnemonic(mnemonic, passphrase, SpanishWords).accountId mustEqual keyPair.accountId
      // #mnemonic-random-spanish
    }

    "an example of deriving non-stellar keys" >> {
      // #mnemonic-french-node-depth
      val mnemonic = Mnemonic.random(FrenchWords)
      val passphrase = new ByteString("chien saucisse".getBytes(UTF_8))
      val rootNode = mnemonic.asHDNode(passphrase)
      val node = rootNode.deriveChild(33, 18, 193, 4)
      node.privateKey mustEqual rootNode
        .deriveChild(33)
        .deriveChild(18)
        .deriveChild(193)
        .deriveChild(4)
        .privateKey
      // #mnemonic-french-node-depth
    }

    "fail to be created with an incorrect phrase size" >> {
      Mnemonic(List("all", "hour", "make", "first", "leader")) must throwAn[IllegalArgumentException]
    }

    "fail to be created with words from outside the list" >> {
      Mnemonic(List("all", "hour", "make", "first", "beaver", "leader")) must throwAn[IllegalArgumentException]
    }

    "fail to be validate with incorrect checksum" >> {
      Mnemonic(List("letter", "advice", "cage", "absurd", "amount", "doctor", "acoustic", "avoid",
        "letter", "advice", "cage", "frog")).validateChecksum() must throwAn[AssertionError]
    }

    "fail to generate randomly with invalid entropy bit lengths" >> {
      Mnemonic.random(entropyBits = 64) must throwAn[IllegalArgumentException]
      Mnemonic.random(entropyBits = 138) must throwAn[IllegalArgumentException]
      Mnemonic.random(entropyBits = 288) must throwAn[IllegalArgumentException]
    }
  }
}

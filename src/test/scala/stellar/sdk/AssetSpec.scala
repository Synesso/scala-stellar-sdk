package stellar.sdk

import org.specs2.mutable.Specification

import scala.util.Try

class AssetSpec extends Specification with ArbitraryInput {

  "an empty code" should {
    "not allow non-native asset construction" >> {
      val issuer = KeyPair.random
      Try(Asset("", issuer)) must beFailedTry[Asset]
      Try(IssuedAsset4.of("", issuer)) must beFailedTry[Asset]
      Try(IssuedAsset12.of("", issuer)) must beFailedTry[Asset]
    }
  }

  "an up-to-4-char code" should {
    "result in a 4-char non-native asset" >> prop { code: String =>
      val issuer = KeyPair.random
      Asset(code, issuer) must beLike {
        case a: IssuedAsset4 =>
          a.code mustEqual code
          a.issuer.accountId mustEqual issuer.accountId
      }
    }.setGen(genCode(1, 4))
  }

  "an 5-to-12-char code" should {
    "result in a 12-char non-native asset" >> prop { code: String =>
      val issuer = KeyPair.random
      Asset(code, issuer) must beLike {
        case a: IssuedAsset12 =>
          a.code mustEqual code
          a.issuer.accountId mustEqual issuer.accountId
      }
    }.setGen(genCode(5, 12))
  }

  "a greater than 12 character code" should {
    "not allow non-native asset construction" >> prop { code: String =>
      val issuer = KeyPair.random
      Try(Asset(code, issuer)) must beFailedTry[Asset]
      Try(IssuedAsset4.of(code, issuer)) must beFailedTry[Asset]
      Try(IssuedAsset12.of(code, issuer)) must beFailedTry[Asset]
    }.setGen(genCode(13, 1000))
  }

  "a code with invalid characters" should {
    "not result in an asset" >> {
      Try(Asset("Why_Under", KeyPair.random)) must beFailedTry[Asset]
      Try(Asset("Hi!", KeyPair.random)) must beFailedTry[Asset]
    }
  }

  "creating an amount with a non-native asset" should {
    "use the specified asset" >> prop { (bal: Long, asset: NonNativeAsset) =>
      Amount(bal, asset).asset mustEqual asset
    }
  }

  "any asset" should {
    "serde via xdr bytes" >> prop { expected: Asset =>
      val (remaining, actual) = Asset.decode.run(expected.encode).value
      actual mustEqual expected
      remaining must beEmpty
    }
  }
}

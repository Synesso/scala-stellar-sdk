package stellar.sdk

import org.specs2.mutable.Specification

class AssetSpec extends Specification with ArbitraryInput {

  "the native asset type" should {
    "serde correctly" >> {
      Asset.fromXDR(NativeAsset.toXDR) must beSuccessfulTry[Asset](NativeAsset)
    }
  }

  "non-native asset types" should {
    "serde correctly" >> prop { code: String =>
      val issuer = KeyPair.random
      val asset = Asset.createNonNative(code, issuer).get
      Asset.fromXDR(asset.toXDR) must beSuccessfulTry[Asset].like {
        case a: IssuedAsset4 if code.length <= 4 =>
          a.code mustEqual code
          a.issuer.accountId mustEqual issuer.accountId
        case a: IssuedAsset12 =>
          a.code mustEqual code
          a.issuer.accountId mustEqual issuer.accountId
      }
    }.setGen(genCode(1, 12))
  }

  "an empty code" should {
    "not allow non-native asset construction" >> {
      val issuer = KeyPair.random
      Asset.createNonNative("", issuer) must beFailedTry[Asset]
      IssuedAsset4("", issuer) must throwAn[AssertionError]
      IssuedAsset12("", issuer) must throwAn[AssertionError]
    }
  }

  "an up-to-4-char code" should {
    "result in a 4-char non-native asset" >> prop { code: String =>
      val issuer = KeyPair.random
      Asset.createNonNative(code, issuer) must beSuccessfulTry[Asset].like {
        case a: IssuedAsset4 =>
          a.code mustEqual code
          a.issuer.accountId mustEqual issuer.accountId
      }
    }.setGen(genCode(1, 4))

    "successfully create a 4-char non-native asset directly" >> prop { code: String =>
      val issuer = KeyPair.random
      val a = IssuedAsset4(code, issuer)
      a.code mustEqual code
      a.issuer.accountId mustEqual issuer.accountId
    }.setGen(genCode(1, 4))

    "fail to create a 12-char non-native asset directly" >> prop { code: String =>
      val issuer = KeyPair.random
      IssuedAsset12(code, issuer) must throwAn[AssertionError]
    }.setGen(genCode(1, 4))
  }

  "an 5-to-12-char code" should {
    "result in a 12-char non-native asset" >> prop { code: String =>
      val issuer = KeyPair.random
      Asset.createNonNative(code, issuer) must beSuccessfulTry[Asset].like {
        case a: IssuedAsset12 =>
          a.code mustEqual code
          a.issuer.accountId mustEqual issuer.accountId
      }
    }.setGen(genCode(5, 12))

    "fail to create a 4-char non-native asset directly" >> prop { code: String =>
      val issuer = KeyPair.random
      IssuedAsset4(code, issuer) must throwAn[AssertionError]
    }.setGen(genCode(5, 12))

    "successfully create a 12-char non-native asset directly" >> prop { code: String =>
      val issuer = KeyPair.random
      val a = IssuedAsset12(code, issuer)
      a.code mustEqual code
      a.issuer.accountId mustEqual issuer.accountId
    }.setGen(genCode(5, 12))
  }

  "a greater than 12 character code" should {
    "not allow non-native asset construction" >> prop { code: String =>
      val issuer = KeyPair.random
      Asset.createNonNative(code, issuer) must beFailedTry[Asset]
      IssuedAsset4(code, issuer) must throwAn[AssertionError]
      IssuedAsset12(code, issuer) must throwAn[AssertionError]
    }.setGen(genCode(13, 1000))
  }

  "creating an amount with a non-native asset" should {
    "use the specified asset" >> prop { (bal: Long, asset: NonNativeAsset) =>
      Amount(bal, asset).asset mustEqual asset
    }
  }
}

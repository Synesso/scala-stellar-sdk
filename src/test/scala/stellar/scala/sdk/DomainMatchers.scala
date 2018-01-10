package stellar.scala.sdk

import org.apache.commons.codec.binary.Hex
import org.specs2.matcher.{AnyMatchers, Matcher, MustExpectations}

trait DomainMatchers extends AnyMatchers with MustExpectations {

  def beEquivalentTo(other: Asset): Matcher[Asset] = beLike[Asset] {
    case AssetTypeNative =>
      other mustEqual AssetTypeNative
    case AssetTypeCreditAlphaNum4(code, issuer) =>
      val AssetTypeCreditAlphaNum4(expectedCode, expectedIssuer) = other
      code mustEqual expectedCode
      issuer.accountId mustEqual expectedIssuer.accountId
    case AssetTypeCreditAlphaNum12(code, issuer) =>
      val AssetTypeCreditAlphaNum12(expectedCode, expectedIssuer) = other
      code mustEqual expectedCode
      issuer.accountId mustEqual expectedIssuer.accountId
  }

  def beEquivalentTo(other: Amount): Matcher[Amount] = beLike[Amount] {
    case NativeAmount(units) =>
      units mustEqual other.units
      other.asset mustEqual AssetTypeNative
    case IssuedAmount(units, asset) =>
      units mustEqual other.units
      asset must beEquivalentTo(other.asset)
  }

  def beEquivalentTo(other: KeyPair): Matcher[KeyPair] = beLike[KeyPair] {
    case KeyPair(pk, sk) =>
      Hex.encodeHex(pk.getAbyte) mustEqual Hex.encodeHex(other.pk.getAbyte)
      Hex.encodeHex(sk.getAbyte) mustEqual Hex.encodeHex(other.sk.getAbyte)
  }

  def beEquivalentTo(other: VerifyingKey): Matcher[VerifyingKey] = beLike[VerifyingKey] {
    case VerifyingKey(pk) =>
      Hex.encodeHex(pk.getAbyte) mustEqual Hex.encodeHex(other.pk.getAbyte)
  }
}

package stellar.scala.sdk

import org.apache.commons.codec.binary.Hex
import org.specs2.matcher.{AnyMatchers, Matcher, MustExpectations}
import org.stellar.sdk.xdr.{Hash, SignerKey, Uint64, Memo => XDRMemo}

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

  def beEquivalentTo(other: SignerKey): Matcher[SignerKey] = beLike[SignerKey] {
    case signer: SignerKey =>
      signer.getDiscriminant mustEqual other.getDiscriminant
      signer.getEd25519.getUint256.toSeq mustEqual other.getEd25519.getUint256.toSeq
  }

  def beEquivalentTo(other: VerifyingKey): Matcher[VerifyingKey] = beLike[VerifyingKey] {
    case VerifyingKey(pk) =>
      Hex.encodeHex(pk.getAbyte) mustEqual Hex.encodeHex(other.pk.getAbyte)
  }

  def beEquivalentTo(other: Hash): Matcher[Hash] = beLike[Hash] {
    case hash =>
      if (other == null) hash must beNull
      else hash.getHash.toSeq mustEqual other.getHash.toSeq
  }

  def beEquivalentTo(other: Uint64): Matcher[Uint64] = beLike[Uint64] {
    case id =>
      if (other == null) id must beNull
      else other.getUint64 mustEqual id.getUint64
  }

  def beEquivalentTo(other: XDRMemo): Matcher[XDRMemo] = beLike[XDRMemo] {
    case memo =>
      memo.getDiscriminant mustEqual other.getDiscriminant
      memo.getHash must beEquivalentTo(other.getHash)
      memo.getId must beEquivalentTo(other.getId)
      memo.getRetHash must beEquivalentTo(other.getRetHash)
      memo.getText mustEqual other.getText
  }
}

package stellar.sdk.model

import java.nio.ByteBuffer

import org.apache.commons.codec.binary.Base32
import org.stellar.xdr.{PublicKey => XPublicKey, _}
import stellar.sdk.model.StrKey.codec
import stellar.sdk.util.ByteArrays
import stellar.sdk.{KeyPair, PublicKey}


/**
  * A StrKey (Stellar Key) is a typed, encoded byte array.
  */
sealed trait StrKey {
  val kind: Byte
  val hash: Seq[Byte]
  def checksum: Seq[Byte] = ByteArrays.checksum((kind +: hash).toArray).toIndexedSeq
  def encodeToChars: Seq[Char] = codec.encode((kind +: hash ++: checksum).toArray).map(_.toChar).toIndexedSeq
}

/**
  * Only a subset of StrKeys can be signers. Seeds should not be the declared signer
  * (as they are the private dual of the AccountId).
  */
sealed trait SignerStrKey extends StrKey {
  def xdr: SignerKey
}

object SignerStrKey {
  def decode(xdr: SignerKey): SignerStrKey = xdr.getDiscriminant match {
    case SignerKeyType.SIGNER_KEY_TYPE_ED25519 => AccountId(xdr.getEd25519.getUint256)
    case SignerKeyType.SIGNER_KEY_TYPE_HASH_X => SHA256Hash(xdr.getHashX.getUint256)
    case SignerKeyType.SIGNER_KEY_TYPE_PRE_AUTH_TX => PreAuthTx(xdr.getPreAuthTx.getUint256)
  }
}

case class AccountId(hash: Seq[Byte], subAccountId: Option[Long] = None) extends SignerStrKey {
  val kind: Byte = subAccountId match {
    case None => (6 << 3).toByte // G
    case _ => (12 << 3).toByte   // M
  }

  override def xdr: SignerKey = new SignerKey.Builder()
    .discriminant(SignerKeyType.SIGNER_KEY_TYPE_ED25519)
    .ed25519(new Uint256(hash.toArray))
    .build()

  def accountIdXdr: AccountID = new AccountID(new XPublicKey.Builder()
      .discriminant(PublicKeyType.PUBLIC_KEY_TYPE_ED25519)
      .ed25519(new Uint256(hash.toArray))
      .build())

  def muxedXdr: MuxedAccount = {
    val builder = new MuxedAccount.Builder()
    subAccountId match {
      case None =>
        builder
          .discriminant(CryptoKeyType.KEY_TYPE_ED25519)
          .ed25519(new Uint256(hash.toArray))
      case Some(id) =>
        builder
          .discriminant(CryptoKeyType.KEY_TYPE_MUXED_ED25519)
          .med25519(new MuxedAccount.MuxedAccountMed25519.Builder()
            .id(new Uint64(id))
            .ed25519(new Uint256(hash.toArray))
            .build())
    }
    builder.build()
  }

/*
  override def encodeToChars: Seq[Char] = subAccountId match {
    case None => super.encodeToChars
    case Some(id) => codec.encode((kind +: long(id) ++: hash ++: checksum).toArray).map(_.toChar).toIndexedSeq
  }

  override def checksum: Seq[Byte] = subAccountId match {
    case None => ByteArrays.checksum((kind +: hash).toArray).toIndexedSeq
    case Some(id) => ByteArrays.checksum((kind +: long(id) ++: hash).toArray).toIndexedSeq
  }
*/

  val isMulitplexed: Boolean = subAccountId.isDefined
  def publicKey: PublicKey = KeyPair.fromPublicKey(hash)

  override def toString: String = s"AccountId(${KeyPair.fromPublicKey(hash).accountId}, $subAccountId)"
}

object AccountId {
  def decode(accountId: AccountID): AccountId =
    AccountId(KeyPair.fromPublicKey(accountId.getAccountID.getEd25519.getUint256).publicKey)

  def decode(muxedAccount: MuxedAccount): AccountId =
    muxedAccount.getDiscriminant match {
      case CryptoKeyType.KEY_TYPE_ED25519 =>
        AccountId(KeyPair.fromPublicKey(muxedAccount.getEd25519.getUint256).publicKey)
      case CryptoKeyType.KEY_TYPE_MUXED_ED25519 =>
        AccountId(
          hash = KeyPair.fromPublicKey(muxedAccount.getMed25519.getEd25519.getUint256).publicKey,
          subAccountId = Some(muxedAccount.getMed25519.getId.getUint64)
        )
      case d => throw new IllegalArgumentException(s"Cannot decode $d into an AccountId")
    }
}

case class Seed(hash: Seq[Byte]) extends StrKey {
  val kind: Byte = (18 << 3).toByte // S
}

case class PreAuthTx(hash: Seq[Byte]) extends SignerStrKey {
  val kind: Byte = (19 << 3).toByte // T
  override def xdr: SignerKey = new SignerKey.Builder()
    .discriminant(SignerKeyType.SIGNER_KEY_TYPE_PRE_AUTH_TX)
    .preAuthTx(new Uint256(hash.toArray))
    .build()
}

case class SHA256Hash(hash: Seq[Byte]) extends SignerStrKey {
  val kind: Byte = (23 << 3).toByte // X
  override def xdr: SignerKey = new SignerKey.Builder()
    .discriminant(SignerKeyType.SIGNER_KEY_TYPE_HASH_X)
    .hashX(new Uint256(hash.toArray))
    .build()
}

object StrKey {

  val codec = new Base32()

  def decodeFromString(key: String): StrKey = decodeFromChars(key.toIndexedSeq)

  def decodeFromChars(key: Seq[Char]): StrKey = {
    assert(key.nonEmpty, "Key cannot be empty")
    assert(key.forall(_ <= 127), "Illegal characters in provided StrKey")
    val bytes = key.map(_.toByte).toArray
    val decoded: Array[Byte] = codec.decode(bytes)
    if (key.head == 'M') decodeMuxedFromChars(decoded) else {
      assert(decoded.length == 35, s"Incorrect length. Expected 35 bytes, got ${decoded.length} in StrKey: $key")

      val data = decoded.tail.take(32)
      val Array(sumA, sumB) = decoded.drop(33)
      val Array(checkA, checkB) = ByteArrays.checksum(decoded.take(33))
      assert((checkA, checkB) == (sumA, sumB),
        f"Checksum does not match. Provided: 0x$sumA%04X,0x$sumB%04X. Actual: 0x$checkA%04X,0x$checkB%04X")

      key.head match {
        case 'G' => AccountId(data.toIndexedSeq)
        case 'S' => Seed(data.toIndexedSeq)
        case 'T' => PreAuthTx(data.toIndexedSeq)
        case 'X' => SHA256Hash(data.toIndexedSeq)
      }
    }
  }

  private def decodeMuxedFromChars(decoded: Array[Byte]): AccountId = {
    val (data, Array(sumA, sumB)) = decoded.tail.splitAt(decoded.length - 3)
    val Array(checkA, checkB) = ByteArrays.checksum(decoded.take(decoded.length - 2))
    assert((checkA, checkB) == (sumA, sumB),
      f"Checksum does not match. Provided: 0x$sumA%04X,0x$sumB%04X. Actual: 0x$checkA%04X,0x$checkB%04X")
    val (accountId, hash) = data.splitAt(8)
    AccountId(hash.toIndexedSeq, Some(ByteBuffer.wrap(accountId).getLong()))
  }
}

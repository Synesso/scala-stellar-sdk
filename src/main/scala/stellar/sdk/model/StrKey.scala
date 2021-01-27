package stellar.sdk.model

import java.nio.ByteBuffer

import cats.data.State
import org.apache.commons.codec.binary.Base32
import org.stellar.xdr.{AccountID, PublicKey => XPublicKey, PublicKeyType, SignerKey, SignerKeyType, Uint256}
import stellar.sdk.{KeyPair, PublicKey}
import stellar.sdk.model.StrKey.codec
import stellar.sdk.model.xdr.Encode.{bytes, int, long}
import stellar.sdk.model.xdr.{Decode, Encodable}
import stellar.sdk.util.ByteArrays


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
sealed trait SignerStrKey extends StrKey with Encodable {
  def signerXdr: SignerKey
}

case class AccountId(hash: Seq[Byte], subAccountId: Option[Long] = None) extends SignerStrKey {
  def xdr: AccountID = new AccountID(new XPublicKey.Builder()
    .discriminant(PublicKeyType.PUBLIC_KEY_TYPE_ED25519)
    .ed25519(new Uint256(hash.toArray))
    .build())

  override def signerXdr: SignerKey = new SignerKey.Builder()
    .discriminant(SignerKeyType.SIGNER_KEY_TYPE_ED25519)
    .ed25519(new Uint256(hash.toArray))
    .build()

  val kind: Byte = subAccountId match {
    case None => (6 << 3).toByte // G
    case _ => (12 << 3).toByte   // M
  }

  def encode: LazyList[Byte] = subAccountId match {
    case None => int(0x000) ++ bytes(32, hash)
    case Some(id) => int(0x100) ++ long(id) ++ bytes(32, hash)
  }

  override def encodeToChars: Seq[Char] = subAccountId match {
    case None => super.encodeToChars
    case Some(id) => codec.encode((kind +: long(id) ++: hash ++: checksum).toArray).map(_.toChar).toIndexedSeq
  }

  override def checksum: Seq[Byte] = subAccountId match {
    case None => ByteArrays.checksum((kind +: hash).toArray).toIndexedSeq
    case Some(id) => ByteArrays.checksum((kind +: long(id) ++: hash).toArray).toIndexedSeq
  }

  val isMulitplexed: Boolean = subAccountId.isDefined
  def publicKey: PublicKey = KeyPair.fromPublicKey(hash)

  override def toString: String = s"AccountId(${KeyPair.fromPublicKey(hash).accountId}, $subAccountId)"
}

object SignerStrKey {
  def decodeXdr(xdr: SignerKey): SignerStrKey = xdr.getDiscriminant match {
    case SignerKeyType.SIGNER_KEY_TYPE_ED25519 => AccountId(xdr.getEd25519.getUint256)
    case SignerKeyType.SIGNER_KEY_TYPE_HASH_X => SHA256Hash(xdr.getHashX.getUint256)
    case SignerKeyType.SIGNER_KEY_TYPE_PRE_AUTH_TX => PreAuthTx(xdr.getPreAuthTx.getUint256)
  }
}

object AccountId extends Decode {
  def decodeXdr(id: AccountID): AccountId =
    AccountId(KeyPair.fromPublicKey(id.getAccountID.getEd25519.getUint256).publicKey)

  val decode: State[Seq[Byte], AccountId] = int.flatMap {
    case 0x000 => bytes(32).map(bs => AccountId(bs.toIndexedSeq))
    case 0x100 => for {
      subAccountId <- long
      bs <- bytes(32)
    } yield AccountId(bs.toIndexedSeq, Some(subAccountId))
  }
}

case class Seed(hash: Seq[Byte]) extends StrKey {
  val kind: Byte = (18 << 3).toByte // S
}

case class PreAuthTx(hash: Seq[Byte]) extends SignerStrKey {
  val kind: Byte = (19 << 3).toByte // T
  def encode: LazyList[Byte] = int(0x001) ++ bytes(32, hash)

  override def signerXdr: SignerKey = new SignerKey.Builder()
    .discriminant(SignerKeyType.SIGNER_KEY_TYPE_PRE_AUTH_TX)
    .preAuthTx(new Uint256(hash.toArray))
    .build()
}

case class SHA256Hash(hash: Seq[Byte]) extends SignerStrKey {
  val kind: Byte = (23 << 3).toByte // X
  def encode: LazyList[Byte] = int(0x002) ++ bytes(32, hash)

  override def signerXdr: SignerKey = new SignerKey.Builder()
    .discriminant(SignerKeyType.SIGNER_KEY_TYPE_HASH_X)
    .hashX(new Uint256(hash.toArray))
    .build()
}

object StrKey extends Decode {

  val codec = new Base32()

  val decode: State[Seq[Byte], SignerStrKey] = int.flatMap {
    case 0x000 => bytes(32).map(bs => AccountId(bs.toIndexedSeq))
    case 0x001 => bytes(32).map(bs => PreAuthTx(bs.toIndexedSeq))
    case 0x002 => bytes(32).map(bs => SHA256Hash(bs.toIndexedSeq))
    case 0x100 => for {
      subAccountId <- long
      bs <- bytes(32)
    } yield AccountId(bs.toIndexedSeq, Some(subAccountId))
  }

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

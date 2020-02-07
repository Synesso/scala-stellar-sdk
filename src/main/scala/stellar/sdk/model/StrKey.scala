package stellar.sdk.model

import cats.data.State
import org.apache.commons.codec.binary.Base32
import stellar.sdk.model.StrKey.codec
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays


/**
  * A StrKey (Stellar Key) is a typed, encoded byte array.
  */
sealed trait StrKey {
  val kind: Byte
  val hash: Seq[Byte]
  def checksum: Seq[Byte] = ByteArrays.checksum((kind +: hash).toArray)
  def encodeToChars: Seq[Char] = codec.encode((kind +: hash ++: checksum).toArray).map(_.toChar)
}

/**
  * Only a subset of StrKeys can be signers. Seeds should not be the declared signer
  * (as they are the private dual of the AccountId).
  */
sealed trait SignerStrKey extends StrKey with Encodable

case class AccountId(hash: Seq[Byte]) extends SignerStrKey {
  val kind: Byte = (6 << 3).toByte // G
  def encode: LazyList[Byte] = Encode.int(0) ++ Encode.bytes(32, hash)
}

case class Seed(hash: Seq[Byte]) extends StrKey {
  val kind: Byte = (18 << 3).toByte // S
}

case class PreAuthTx(hash: Seq[Byte]) extends SignerStrKey {
  val kind: Byte = (19 << 3).toByte // T
  def encode: LazyList[Byte] = Encode.int(1) ++ Encode.bytes(32, hash)
}

case class SHA256Hash(hash: Seq[Byte]) extends SignerStrKey {
  val kind: Byte = (23 << 3).toByte // X
  def encode: LazyList[Byte] = Encode.int(2) ++ Encode.bytes(32, hash)
}

object StrKey extends Decode {

  val codec = new Base32()

  def decode: State[Seq[Byte], SignerStrKey] = for {
    discriminant <- int
    bs <- bytes(32).map(_.toArray)
  } yield discriminant match {
    case 0 => AccountId(bs)
    case 1 => PreAuthTx(bs)
    case 2 => SHA256Hash(bs)
  }

  def decodeFromString(key: String): StrKey = decodeFromChars(key.toCharArray)
  def decodeFromChars(key: Seq[Char]): StrKey = {
    assert(key.forall(_ <= 127), s"Illegal characters in provided StrKey")

    val bytes = key.map(_.toByte).toArray
    val decoded: Array[Byte] = codec.decode(bytes)
    assert(decoded.length == 35, s"Incorrect length. Expected 35 bytes, got ${decoded.length} in StrKey: $key")

    val data = decoded.tail.take(32)
    val Array(sumA, sumB) = decoded.drop(33)
    val Array(checkA, checkB) = ByteArrays.checksum(decoded.take(33))
    assert((checkA, checkB) == (sumA, sumB),
      f"Checksum does not match. Provided: 0x$sumA%04X0x$sumB%04X. Actual: 0x$checkA%04X0x$checkB%04X")

    key.head match {
      case 'G' => AccountId(data)
      case 'S' => Seed(data)
      case 'T' => PreAuthTx(data)
      case 'X' => SHA256Hash(data)
    }
  }
}

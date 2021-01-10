package stellar.sdk.model

import okio.ByteString
import org.stellar.xdr.{Hash, MemoType, Uint64, XdrString, Memo => XMemo}
import stellar.sdk.util.ByteArrays._

import scala.util.Try

sealed trait Memo {
  def xdr: XMemo
}

case object NoMemo extends Memo {
  override def xdr: XMemo = new XMemo.Builder()
    .discriminant(MemoType.MEMO_NONE)
    .build()
}

case class MemoText(byteString: ByteString) extends Memo {
  val Length = 28
  val bytes: Array[Byte] = byteString.toByteArray
  val text: String = byteString.utf8()
  assert(byteString.size() <= Length, s"Text exceeded limit (${byteString.size()}/$Length bytes)")

  override def xdr: XMemo =
    new XMemo.Builder()
      .discriminant(MemoType.MEMO_TEXT)
      .text(new XdrString(bytes))
      .build()
}

object MemoText {
  def apply(text: String): MemoText = MemoText(ByteString.encodeUtf8(text))
}

case class MemoId(id: Long) extends Memo {
  override def xdr: XMemo =
    new XMemo.Builder()
      .discriminant(MemoType.MEMO_ID)
      .id(new Uint64(id))
      .build()

  def unsignedId: BigInt = BigInt(java.lang.Long.toUnsignedString(id))

  override def toString = s"MemoId(${unsignedId.toString()})"
}

sealed trait MemoWithHash extends Memo {
  val Length = 32
  val bs: Seq[Byte]
  val bytes: Array[Byte] = paddedByteArray(bs.toArray, Length)

  def hex: String = bytesToHex(bytes)

  def hexTrim: String = bytesToHex(bs)
}

case class MemoHash(bs: Seq[Byte]) extends MemoWithHash {
  assert(bs.length <= Length, s"Hash exceeded limit (${bytes.length}/$Length bytes)")

  override def xdr: XMemo =
    new XMemo.Builder()
      .discriminant(MemoType.MEMO_HASH)
      .hash(new Hash(bs.toArray))
      .build()
}

object MemoHash {
  def from(hex: String): Try[MemoHash] = Try(MemoHash(hexToBytes(hex)))
}

case class MemoReturnHash(bs: Seq[Byte]) extends MemoWithHash {
  assert(bs.length <= Length, s"Hash exceeded limit (${bytes.length}/$Length bytes)")

  override def xdr: XMemo =
    new XMemo.Builder()
      .discriminant(MemoType.MEMO_RETURN)
      .retHash(new Hash(bs.toArray))
      .build()
}

object MemoReturnHash {
  def from(hex: String): Try[MemoReturnHash] = Try(MemoReturnHash(hexToBytes(hex)))
}

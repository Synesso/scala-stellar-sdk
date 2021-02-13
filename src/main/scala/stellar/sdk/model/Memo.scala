package stellar.sdk.model

import okio.ByteString
import org.stellar.xdr.{Hash, MemoType, Uint64, XdrString, Memo => XMemo}
import stellar.sdk.util.ByteArrays._

import scala.util.Try

sealed trait Memo {
  def xdr: XMemo
  def encode: LazyList[Byte] = LazyList.from(xdr.encode().toByteArray)
}

object Memo {
  def decodeXdr(xdr: XMemo): Memo = xdr.getDiscriminant match {
    case MemoType.MEMO_NONE => NoMemo
    case MemoType.MEMO_ID => MemoId(xdr.getId.getUint64)
    case MemoType.MEMO_TEXT => MemoText(new ByteString(xdr.getText.getBytes))
    case MemoType.MEMO_HASH => MemoHash(new ByteString(xdr.getHash.getHash))
    case MemoType.MEMO_RETURN => MemoReturnHash(new ByteString(xdr.getRetHash.getHash))
  }
}

case object NoMemo extends Memo {
  override def xdr: XMemo =
    new XMemo.Builder()
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
  val bs: ByteString
  val bytes: Array[Byte] = bs.toByteArray

  def hex: String = bs.hex()
}

case class MemoHash(bs: ByteString) extends MemoWithHash {
  assert(bs.size() == Length, s"Hash has incorrect length (${bs.size()}/$Length bytes)")

  override def xdr: XMemo =
    new XMemo.Builder()
      .discriminant(MemoType.MEMO_HASH)
      .hash(new Hash(bs.toByteArray))
      .build()
}

object MemoHash {
  def from(hex: String): Try[MemoHash] = Try(MemoHash(ByteString.decodeHex(hex)))
  def apply(bs: Array[Byte]): MemoHash = MemoHash(new ByteString(bs))
}

case class MemoReturnHash(bs: ByteString) extends MemoWithHash {
  assert(bs.size() == Length, s"Hash has incorrect length (${bs.size()}/$Length bytes)")

  override def xdr: XMemo =
    new XMemo.Builder()
      .discriminant(MemoType.MEMO_RETURN)
      .retHash(new Hash(bs.toByteArray))
      .build()
}

object MemoReturnHash {
  def from(hex: String) = Try(MemoReturnHash(ByteString.decodeHex(hex)))
  def apply(bs: Array[Byte]): MemoReturnHash = MemoReturnHash(new ByteString(bs))
}

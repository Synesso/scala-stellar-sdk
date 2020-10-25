package stellar.sdk.model

import java.nio.charset.StandardCharsets.UTF_8

import cats.data.State
import okio.ByteString
import stellar.sdk.util.ByteArrays._
import stellar.sdk.model.xdr.{Decode, Encode}

import scala.util.Try

sealed trait Memo {
  def encode: LazyList[Byte]
}

object Memo extends Decode {
  def decode: State[Seq[Byte], Memo] = switch(
    State.pure(NoMemo),
    string.map(MemoText(_)),
    long.map(MemoId),
    bytes.map(_.toIndexedSeq).map(MemoHash(_)),
    bytes.map(_.toIndexedSeq).map(MemoReturnHash(_))
  )
}

case object NoMemo extends Memo {
  override def encode: LazyList[Byte] = Encode.int(0)
}

case class MemoText(byteString: ByteString) extends Memo {
  val Length = 28
  val bytes = byteString.toByteArray
  val text: String = byteString.utf8()
  assert(byteString.size() <= Length, s"Text exceeded limit (${byteString.size()}/$Length bytes)")

  override def encode: LazyList[Byte] = Encode.int(1) ++ Encode.string(text)
}

object MemoText {
  def apply(text: String): MemoText = MemoText(ByteString.encodeUtf8(text))
}

case class MemoId(id: Long) extends Memo {
  override def encode: LazyList[Byte] = Encode.int(2) ++ Encode.long(id)

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

  override def encode: LazyList[Byte] = Encode.int(3) ++ Encode.bytes(bs)
}

object MemoHash {
  def from(hex: String): Try[MemoHash] = Try(MemoHash(hexToBytes(hex)))
}

case class MemoReturnHash(bs: Seq[Byte]) extends MemoWithHash {
  assert(bs.length <= Length, s"Hash exceeded limit (${bytes.length}/$Length bytes)")

  override def encode: LazyList[Byte] = Encode.int(4) ++ Encode.bytes(bs)
}

object MemoReturnHash {
  def from(hex: String) = Try(MemoReturnHash(hexToBytes(hex)))
}

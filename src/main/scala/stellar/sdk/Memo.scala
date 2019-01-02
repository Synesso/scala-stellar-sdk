package stellar.sdk

import java.nio.charset.StandardCharsets.UTF_8

import cats.data.State
import stellar.sdk.ByteArrays._
import stellar.sdk.model.xdr.{Decode, Encode}

import scala.util.Try

sealed trait Memo {
  def encode: Stream[Byte]
}

object Memo {
  def decode: State[Seq[Byte], Memo] = Decode.int.flatMap {
    case 0 => State.pure(NoMemo)
    case 1 => Decode.string.map(MemoText)
    case 2 => Decode.long.map(MemoId)
    case 3 => Decode.bytes.map(_.toArray).map(MemoHash(_))
    case 4 => Decode.bytes.map(_.toArray).map(MemoReturnHash(_))
  }
}

case object NoMemo extends Memo {
  override def encode: Stream[Byte] = Encode.int(0)
}

case class MemoText(text: String) extends Memo {
  val Length = 28
  val bytes = text.getBytes(UTF_8)
  assert(bytes.length <= Length, s"Text exceeded limit (${bytes.length}/$Length bytes)")

  override def encode: Stream[Byte] = Encode.int(1) ++ Encode.string(text)
}

case class MemoId(id: Long) extends Memo {
  assert(id > 0, s"Id must be positive (not $id)")

  override def encode: Stream[Byte] = Encode.int(2) ++ Encode.long(id)
}

trait MemoWithHash extends Memo {
  val Length = 32
  val bs: Array[Byte]
  val bytes = paddedByteArray(bs, Length)

  def hex: String = bytesToHex(bytes)

  def hexTrim: String = bytesToHex(bs)
}

case class MemoHash(bs: Array[Byte]) extends MemoWithHash {
  assert(bs.length <= Length, s"Hash exceeded limit (${bytes.length}/$Length bytes)")

  override def encode: Stream[Byte] = Encode.int(3) ++ Encode.bytes(bs)
}

object MemoHash {
  def from(hex: String): Try[MemoHash] = Try(MemoHash(hexToBytes(hex)))
}

case class MemoReturnHash(bs: Array[Byte]) extends MemoWithHash {
  assert(bs.length <= Length, s"Hash exceeded limit (${bytes.length}/$Length bytes)")

  override def encode: Stream[Byte] = Encode.int(4) ++ Encode.bytes(bs)
}

object MemoReturnHash {
  def from(hex: String) = Try(MemoReturnHash(hexToBytes(hex)))
}

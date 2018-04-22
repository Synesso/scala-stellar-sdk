package stellar.sdk

import java.nio.charset.StandardCharsets.UTF_8

import org.stellar.sdk.xdr.MemoType._
import org.stellar.sdk.xdr.{Memo => XDRMemo}
import stellar.sdk.ByteArrays._
import stellar.sdk.XDRPrimitives._

import scala.util.Try

sealed trait Memo {
  def toXDR: XDRMemo
}

object Memo {
  def fromXDR(memo: XDRMemo): Memo = {
    memo.getDiscriminant match {
      case MEMO_NONE => NoMemo
      case MEMO_TEXT => MemoText(memo.getText)
      case MEMO_ID => MemoId(memo.getId.getUint64)
      case MEMO_HASH => MemoHash(trimmedByteArray(memo.getHash.getHash))
      case MEMO_RETURN => MemoReturnHash(trimmedByteArray(memo.getRetHash.getHash))
    }
  }
}

case object NoMemo extends Memo {
  override def toXDR: XDRMemo = {
    val m = new XDRMemo
    m.setDiscriminant(MEMO_NONE)
    m
  }
}

case class MemoText(text: String) extends Memo {
  val Length = 28
  val bytes = text.getBytes(UTF_8)
  assert(bytes.length <= Length, s"Text exceeded limit (${bytes.length}/$Length bytes)")

  override def toXDR: XDRMemo = {
    val m = new XDRMemo
    m.setDiscriminant(MEMO_TEXT)
    m.setText(text)
    m
  }
}

case class MemoId(id: Long) extends Memo {
  assert(id > 0, s"Id must be positive (not $id)")

  override def toXDR: XDRMemo = {
    val m = new XDRMemo
    m.setDiscriminant(MEMO_ID)
    m.setId(uint64(id))
    m
  }
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

  override def toXDR: XDRMemo = {
    val m = new XDRMemo
    m.setDiscriminant(MEMO_HASH)
    m.setHash(hash(bytes))
    m
  }
}

object MemoHash {
  def from(hex: String): Try[MemoHash] = Try(MemoHash(hexToBytes(hex)))
}

case class MemoReturnHash(bs: Array[Byte]) extends MemoWithHash {
  assert(bs.length <= Length, s"Hash exceeded limit (${bytes.length}/$Length bytes)")

  override def toXDR: XDRMemo = {
    val m = new XDRMemo
    m.setDiscriminant(MEMO_RETURN)
    m.setRetHash(hash(bytes))
    m
  }
}

object MemoReturnHash {
  def from(hex: String) = Try(MemoReturnHash(hexToBytes(hex)))
}

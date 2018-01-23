package stellar.scala.sdk

import org.stellar.sdk.xdr.MemoType._
import org.stellar.sdk.xdr.{MemoType, Memo => XDRMemo}
import stellar.scala.sdk.MemoHash.hexToBytes

import scala.util.Try

sealed trait Memo {
  def toXDR: XDRMemo = ???
}

case object NoMemo extends Memo {
  override def toXDR: XDRMemo = {
    val m = new XDRMemo
    m.setDiscriminant(MEMO_NONE)
    m
  }
}

case class MemoText(text: String) extends Memo

case class MemoId(id: Long) extends Memo

trait MemoWithHash extends Memo with ByteArrays with XDRPrimitives {
  val Length = 32
  val bs: Array[Byte]
  val bytes = paddedByteArray(bs, Length)
  def hex: String = bytesToHex(bytes)
  def hexTrim: String = bytesToHex(bs)
  private[sdk] def toXDR(discriminant: MemoType): XDRMemo = {
    val m = new XDRMemo
    m.setDiscriminant(discriminant)
    m.setHash(hash(bytes))
    m
  }
}

case class MemoHash(bs: Array[Byte]) extends MemoWithHash {
  assert(bs.length <= Length, s"Hash exceeded limit ($Length bytes)")
  override def toXDR: XDRMemo = toXDR(MEMO_HASH)
}

object MemoHash extends ByteArrays {
  def from(hex: String): Try[MemoHash] = Try(MemoHash(hexToBytes(hex)))
}

case class MemoReturnHash(bs: Array[Byte]) extends MemoWithHash {
  assert(bs.length <= Length, s"Hash exceeded limit ($Length bytes)")
  override def toXDR: XDRMemo = toXDR(MEMO_RETURN)
}

object MemoReturnHash extends ByteArrays {
  def from(hex: String) = Try(MemoReturnHash(hexToBytes(hex)))
}

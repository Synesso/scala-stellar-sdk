package stellar.scala.sdk

import org.stellar.sdk.xdr.MemoType.MEMO_HASH
import org.stellar.sdk.xdr.{MemoType, Memo => XDRMemo}

sealed trait Memo {
  def toXDR: XDRMemo = ???
}

case object NoMemo extends Memo

case class MemoText(text: String) extends Memo

case class MemoId(id: Long) extends Memo

case class MemoHash(bs: Array[Byte]) extends Memo with ByteArrays with XDRPrimitives {
  private val Length = 32
  assert(bs.length <= Length, s"Hash exceeded limit ($Length bytes)")
  val bytes = paddedByteArray(bs, 32)
  def hex: String = bytes.map("%02X".format(_)).mkString
  def hexTrim: String = bs.map("%02X".format(_)).mkString
  override def toXDR: XDRMemo = {
    val m = new XDRMemo
    m.setDiscriminant(MEMO_HASH)
    m.setHash(hash(bytes))
    m
  }
}

case class MemoReturnHash(hash: Array[Byte]) extends Memo

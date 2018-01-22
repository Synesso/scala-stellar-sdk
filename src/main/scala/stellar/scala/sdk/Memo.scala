package stellar.scala.sdk

import org.stellar.sdk.xdr.{Memo => XDRMemo}

sealed trait Memo {
  def toXDR: XDRMemo = ???
}

case object NoMemo extends Memo

case class MemoText(text: String) extends Memo

case class MemoId(id: Long) extends Memo

case class MemoHash(hash: Array[Byte]) extends Memo

case class MemoReturnHash(hash: Array[Byte]) extends Memo

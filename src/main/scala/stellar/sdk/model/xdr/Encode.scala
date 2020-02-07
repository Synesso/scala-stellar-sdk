package stellar.sdk.model.xdr

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

trait Encodable {
  def encode: LazyList[Byte]
}

object Encode {

  def int(i: Int): LazyList[Byte] = {
    val buffer = ByteBuffer.allocate(4)
    buffer.putInt(i)
    buffer.array().to(LazyList)
  }

  def long(l: Long): LazyList[Byte] = {
    val buffer = ByteBuffer.allocate(8)
    buffer.putLong(l)
    buffer.array().to(LazyList)
  }

  def instant(i: Instant): LazyList[Byte] = long(i.getEpochSecond)

  def bytes(len: Int, bs: Seq[Byte]): LazyList[Byte] = {
    require(bs.length == len)
    bs.to(LazyList)
  }

  def bytes(bs: Seq[Byte]): LazyList[Byte] = int(bs.length) ++ bs

  def padded(bs: Seq[Byte], multipleOf: Int = 4): LazyList[Byte] = {
    val filler = Array.fill[Byte]((multipleOf - (bs.length % multipleOf)) % multipleOf)(0)
    bytes(bs) ++ filler
  }

  def string(s: String): LazyList[Byte] = padded(s.getBytes(UTF_8).toIndexedSeq)

  def opt(o: Option[Encodable], ifPresent: Int = 1, ifAbsent: Int = 0): LazyList[Byte] =
    o.map(t => int(ifPresent) ++ t.encode).getOrElse(int(ifAbsent))

  private def optT[T](o: Option[T], encode: T => LazyList[Byte]) =
    o.map(encode).map(int(1) ++ _).getOrElse(int(0))

  def optInt(o: Option[Int]): LazyList[Byte] = optT(o, int)

  def optLong(o: Option[Long]): LazyList[Byte] = optT(o, long)

  def optString(o: Option[String]): LazyList[Byte] = optT(o, string)

  def optBytes(o: Option[Seq[Byte]]): LazyList[Byte] = optT(o, bytes)

  def arr(xs: Seq[Encodable]): LazyList[Byte] = int(xs.size) ++ xs.flatMap(_.encode)

  def arrString(xs: Seq[String]): LazyList[Byte] = int(xs.size) ++ xs.flatMap(string)

  def bool(b: Boolean): LazyList[Byte] = if (b) int(1) else int(0)

}

case class Encoded(encode: LazyList[Byte]) extends Encodable
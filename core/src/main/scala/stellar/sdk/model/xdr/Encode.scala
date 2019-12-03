package stellar.sdk.model.xdr

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

trait Encodable {
  def encode: Stream[Byte]
}

object Encode {

  def int(i: Int): Stream[Byte] = {
    val buffer = ByteBuffer.allocate(4)
    buffer.putInt(i)
    buffer.array().toStream
  }

  def long(l: Long): Stream[Byte] = {
    val buffer = ByteBuffer.allocate(8)
    buffer.putLong(l)
    buffer.array().toStream
  }

  def instant(i: Instant): Stream[Byte] = long(i.getEpochSecond)

  def bytes(len: Int, bs: Seq[Byte]): Stream[Byte] = {
    require(bs.length == len)
    bs.toStream
  }

  def bytes(bs: Seq[Byte]): Stream[Byte] = int(bs.length) ++ bs

  def padded(bs: Seq[Byte], multipleOf: Int = 4): Stream[Byte] = {
    val filler = Array.fill[Byte]((multipleOf - (bs.length % multipleOf)) % multipleOf)(0)
    bytes(bs) ++ filler
  }

  def string(s: String): Stream[Byte] = padded(s.getBytes(UTF_8))

  def opt(o: Option[Encodable], ifPresent: Int = 1, ifAbsent: Int = 0): Stream[Byte] =
    o.map(t => int(ifPresent) ++ t.encode).getOrElse(int(ifAbsent))

  private def optT[T](o: Option[T], encode: T => Stream[Byte]) =
    o.map(encode).map(int(1) ++ _).getOrElse(int(0))

  def optInt(o: Option[Int]): Stream[Byte] = optT(o, int)

  def optLong(o: Option[Long]): Stream[Byte] = optT(o, long)

  def optString(o: Option[String]): Stream[Byte] = optT(o, string)

  def optBytes(o: Option[Seq[Byte]]): Stream[Byte] = optT(o, bytes)

  def arr(xs: Seq[Encodable]): Stream[Byte] = int(xs.size) ++ xs.flatMap(_.encode)

  def arrString(xs: Seq[String]): Stream[Byte] = int(xs.size) ++ xs.flatMap(string)

  def bool(b: Boolean): Stream[Byte] = if (b) int(1) else int(0)

}

case class Encoded(encode: Stream[Byte]) extends Encodable
package stellar.sdk.xdr

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Instant

// todo - Every implementing class should have a serde spec
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

  def instant(i: Instant): Stream[Byte] = long(i.toEpochMilli)

  def bytes(len: Int, bs: Seq[Byte]): Stream[Byte] = {
    require(bs.length == len)
    bs.toStream
  }

  def bytes(bs: Seq[Byte]): Stream[Byte] = int(bs.length) ++ bs

  def string(s: String): Stream[Byte] = {
    val bs = s.getBytes(Charset.forName("UTF-8"))
    val filler = Array.fill[Byte]((4 - (bs.length %4)) % 4)(0)
    bytes(bs) ++ filler
  }

  def opt(o: Option[Encodable]): Stream[Byte] = o.map(t => int(1) ++ t.encode).getOrElse(int(0))

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
package stellar.sdk

import java.nio.charset.Charset

trait Encodable {
  def encode: Stream[Byte]
}

object Encode {

  def int(i: Int): Stream[Byte] =
    Stream(
      (i >>> 24) & 0xFF,
      (i >>> 16) & 0xFF,
      (i >>> 8) & 0xFF,
      (i >>> 0) & 0xFF
    ).map(_.toByte)

  def long(l: Long): Stream[Byte] =
    Stream(
      (l >>> 56) & 0xFF,
      (l >>> 48) & 0xFF,
      (l >>> 40) & 0xFF,
      (l >>> 32) & 0xFF,
      (l >>> 24) & 0xFF,
      (l >>> 16) & 0xFF,
      (l >>> 8) & 0xFF,
      (l >>> 0) & 0xFF
    ).map(_.toByte)

  def string(s: String): Stream[Byte] = bytes(s.getBytes(Charset.forName("UTF-8")))

  def bytes(bs: Array[Byte]): Stream[Byte] = int(bs.length) ++ bs

  def opt(o: Option[Encodable]): Stream[Byte] = o.map(e => int(1) ++ e.encode).getOrElse(int(0))

  def optInt(o: Option[Int]): Stream[Byte] = o.map(i => int(1) ++ int(i)).getOrElse(int(0))

  def optString(o: Option[String]): Stream[Byte] = o.map(s => int(1) ++ string(s)).getOrElse(int(0))

  def varArr(xs: Seq[Encodable]): Stream[Byte] = int(xs.length) ++ xs.flatMap(_.encode).toStream

}

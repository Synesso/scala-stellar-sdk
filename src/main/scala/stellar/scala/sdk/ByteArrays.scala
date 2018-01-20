package stellar.scala.sdk

import java.security.MessageDigest

import scala.util.Try

trait ByteArrays {

  def paddedByteArray(bs: Array[Byte], length: Int): Array[Byte] = {
    val padded = Array.ofDim[Byte](math.max(length, bs.length))
    System.arraycopy(bs, 0, padded, 0, bs.length)
    padded
  }

  def paddedByteArray(s: String, length: Int): Array[Byte] = paddedByteArray(s.getBytes, length)

  def paddedByteArrayToString(bs: Array[Byte]): String = new String(bs).split("\u0000")(0)

  def sha256(bs: Array[Byte]): Try[Array[Byte]] = Try {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(bs)
    md.digest
  }

}

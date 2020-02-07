package stellar.sdk.util

import java.security.MessageDigest

import okio.ByteString
import org.apache.commons.codec.binary.Base64

import scala.annotation.tailrec
import scala.language.implicitConversions

object ByteArrays {

  object Implicits {
    implicit def byteArrayToByteString(arr: Array[Byte]): ByteString =
      new ByteString(arr)

    implicit def byteStringToByteArray(byteString: ByteString): Array[Byte] =
      byteString.toByteArray
  }

  def paddedByteArray(bs: Array[Byte], length: Int): Array[Byte] = {
    val padded = Array.ofDim[Byte](math.max(length, bs.length))
    System.arraycopy(bs, 0, padded, 0, bs.length)
    padded
  }

  def paddedByteArray(s: String, length: Int): Array[Byte] = paddedByteArray(s.getBytes("US-ASCII"), length)

  def paddedByteArrayToString(bs: Array[Byte]): String = new String(bs, "US-ASCII").split("\u0000")(0)

  def trimmedByteArray(bs: Array[Byte]): Seq[Byte] = bs.reverse.dropWhile(_ == 0).reverse.toIndexedSeq

  def sha256(bs: Array[Byte]): Array[Byte] = sha256(bs.toIndexedSeq)
  def sha256(bs: Seq[Byte]): Array[Byte] = {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(bs.toArray)
    md.digest
  }

  def base64(bs: Seq[Byte]): String = base64(bs.toArray)
  def base64(bs: Array[Byte]): String = Base64.encodeBase64String(bs)

  def base64(s: String): Array[Byte] = Base64.decodeBase64(s)

  def bytesToHex(bs: Seq[Byte]): String = bs.map("%02X".format(_)).mkString

  def hexToBytes(hex: String): Array[Byte] = hex.toSeq.sliding(2, 2).map(_.unwrap).map(Integer.parseInt(_, 16).toByte).toArray

  def checksum(bytes: Array[Byte]): Array[Byte] = {
    // This code calculates CRC16-XModem checksum
    // Ported from https://github.com/alexgorbatchev/node-crc, via https://github.com/stellar/java-stellar-sdk

    @tailrec
    def loop(bs: Seq[Byte], crc: Int): Int = {
      bs match {
        case h +: t =>
          var code = crc >>> 8 & 0xFF
          code ^= h & 0xFF
          code ^= code >>> 4
          var crc_ = crc << 8 & 0xFFFF
          crc_ ^= code
          code = code << 5 & 0xFFFF
          crc_ ^= code
          code = code << 7 & 0xFFFF
          crc_ ^= code
          loop(t, crc_)
        case Nil => crc
      }
    }

    val crc = loop(bytes.toIndexedSeq, 0x0000)
    Array(crc.toByte, (crc >>> 8).toByte)
  }

}

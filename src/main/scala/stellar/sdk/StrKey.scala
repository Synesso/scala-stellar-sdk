package stellar.sdk

import java.io.ByteArrayOutputStream
import java.util

import org.apache.commons.codec.binary.Base32

import scala.annotation.tailrec

object StrKey {

  sealed trait VersionByte {
    val value: Byte
  }

  case object AccountId extends VersionByte {
    val value: Byte = (6 << 3).toByte // G
  }

  case object Seed extends VersionByte {
    val value: Byte = (18 << 3).toByte // S
  }

/*
  case object PreAuthTx extends VersionByte {
    val value: Byte = (19 << 3).toByte // T
  }

  case object SHA256Hash extends VersionByte {
    val value: Byte = (23 << 3).toByte // X
  }
*/

  def decodeStellarAccountId(data: String): Array[Byte] = decodeCheck(AccountId, data.toCharArray)

  def decodeStellarSecretSeed(data: Array[Char]): Array[Byte] = decodeCheck(Seed, data)

  def encodeStellarAccountId(data: Array[Byte]): String = String.valueOf(encodeCheck(AccountId, data))

  def encodeStellarSecretSeed(data: Array[Byte]): Array[Char] = encodeCheck(Seed, data)

  def decodeCheck(vb: VersionByte, encoded: Array[Char]): Array[Byte] = {
    val bytes = encoded.map { c =>
      if (c > 127) throw new IllegalArgumentException("Illegal characters in encoded char array.")
      c.toByte
    }
    val decoded = new Base32().decode(bytes)
    val decodedVersionByte = decoded.head
    val payload = util.Arrays.copyOfRange(decoded, 0, decoded.length - 2)
    val data = util.Arrays.copyOfRange(payload, 1, payload.length)
    val checksum = util.Arrays.copyOfRange(decoded, decoded.length - 2, decoded.length)

    if (decodedVersionByte != vb.value) {
      throw new FormatException("Version byte is invalid")
    }

    if (!util.Arrays.equals(calculateChecksum(payload), checksum)) {
      throw new FormatException("Checksum invalid")
    }

    if (Seed.value == decodedVersionByte) {
      util.Arrays.fill(bytes, 0.toByte)
      util.Arrays.fill(decoded, 0.toByte)
      util.Arrays.fill(payload, 0.toByte)
    }

    data
  }

  def encodeCheck(vb: VersionByte, data: Array[Byte]): Array[Char] = {
    val os = new ByteArrayOutputStream()
    os.write(vb.value)
    os.write(data)
    val payload = os.toByteArray
    val checksum = calculateChecksum(payload)
    os.write(checksum)
    val notYetEncoded = os.toByteArray
    val bytesEncoded = new Base32().encode(notYetEncoded)
    val charsEncoded = bytesEncoded.map(_.toChar)
    if (vb == Seed) {
      util.Arrays.fill(notYetEncoded, 0.toByte)
      util.Arrays.fill(payload, 0.toByte)
      util.Arrays.fill(bytesEncoded, 0.toByte)
    }
    charsEncoded
  }

  private def calculateChecksum(bytes: Array[Byte]): Array[Byte] = {
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

    val crc = loop(bytes, 0x0000)
    Array(crc.toByte, (crc >>> 8).toByte)
  }


}


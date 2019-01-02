package stellar.sdk.util

import java.math.BigInteger

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.util.ByteArrays._

import scala.util.Try

class ByteArraysSpec extends Specification with ArbitraryInput {

  "padding a byte array" should {
    "do nothing when required length is the array length" >> prop { bs: Array[Byte] =>
      paddedByteArray(bs, bs.length).toSeq mustEqual bs.toSeq
    }

    "do nothing when required length is less than the array length" >> prop { bs: Array[Byte] =>
      paddedByteArray(bs, bs.length - 1).toSeq mustEqual bs.toSeq
    }.setGen(Gen.nonEmptyListOf(Gen.posNum[Byte]).map(_.toArray))

    "pad with zeros when required length is greater than the array length" >> prop { (bs: Array[Byte], plus: Byte) =>
      paddedByteArray(bs, bs.length + plus.toInt).toSeq mustEqual bs.toSeq ++ (1 to plus).map(_ => 0)
    }.setGen2(Gen.posNum[Byte])
  }

  "trimming a byte array" should {
    "remove trailing zeros" >> {
      trimmedByteArray(Array()) mustEqual Array()
      trimmedByteArray("hello".getBytes("UTF-8")) mustEqual "hello".getBytes("UTF-8")
      trimmedByteArray("hello\u0000\u0000".getBytes("UTF-8")) mustEqual "hello".getBytes("UTF-8")
      trimmedByteArray("hello\u0000there".getBytes("UTF-8")) mustEqual "hello\u0000there".getBytes("UTF-8")
    }
  }

  "sha256" should {
    "hash correctly" >> {
      val hash = sha256("今日は世界".getBytes("UTF-8"))
      new BigInteger(1, hash).toString(16).toUpperCase mustEqual
        "72C2CC3C678D77939435E5AE0A0EF2B83D6A42AFB221EA15CD736CB122B23989"
    }
    "hash anything" >> prop { bs: Array[Byte] =>
      Try(sha256(bs)) must beSuccessfulTry[Array[Byte]]
    }
  }

  "base64" should {
    "perform round trip to string" >> prop { bs: Array[Byte] =>
      base64(base64(bs)).toSeq mustEqual bs.toSeq
    }
  }

}

package stellar.scala.sdk

import java.math.BigInteger

import org.scalacheck.Gen
import org.specs2.mutable.Specification

class ByteArraysSpec extends Specification with ArbitraryInput with ByteArrays {

  "padding a byte array" should {
    "do nothing when required length is the array length" >> prop { bs: Array[Byte] =>
      paddedByteArray(bs, bs.length).toSeq mustEqual bs.toSeq
    }

    "do nothing when required length is less than the array length" >> prop { bs: Array[Byte] =>
      paddedByteArray(bs, bs.length - 1).toSeq mustEqual bs.toSeq
    }.setGen(Gen.nonEmptyListOf(Gen.posNum[Byte]).map(_.toArray))

    "pad with zeros when required length is greater than the array length" >> prop { (bs : Array[Byte], plus: Byte) =>
      paddedByteArray(bs, bs.length + plus.toInt).toSeq mustEqual bs.toSeq ++ (1 to plus).map(_ => 0)
    }.setGen2(Gen.posNum[Byte])
  }

  "sha256" should {
    "hash correctly" >> {
      sha256("今日は世界".getBytes).map(new BigInteger(1, _).toString(16).toUpperCase) must
        beSuccessfulTry("72C2CC3C678D77939435E5AE0A0EF2B83D6A42AFB221EA15CD736CB122B23989")
    }
    "hash anything" >> prop { bs: Array[Byte] =>
      sha256(bs) must beSuccessfulTry[Array[Byte]]
    }
  }

}

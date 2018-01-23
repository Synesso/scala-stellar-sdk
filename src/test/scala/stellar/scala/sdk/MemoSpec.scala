package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.MemoType._

class MemoSpec extends Specification with ArbitraryInput with ByteArrays {

  "a memo hash" should {
    "not be constructable with > 32 bytes" >> {
      MemoHash((1 to 33).map(_.toByte).toArray) must throwAn[AssertionError]
    }

    "pad the input bytes" >> prop { bs: Array[Byte] =>
      MemoHash(bs.take(32)).bytes mustEqual paddedByteArray(bs.take(32), 32)
    }

    "provide padded hex value" >> prop { s: String =>
      val hex = MemoHash(s.take(32).getBytes("UTF-8")).hex
      new String(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte), "UTF-8").trim mustEqual s.take(32)
      hex.length mustEqual 64
    }.setGen(Gen.identifier)

    "provide trimmed hex value" >> prop { s: String =>
      val hex = MemoHash(s.take(32).getBytes("UTF-8")).hexTrim
      new String(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte), "UTF-8") mustEqual s.take(32)
    }.setGen(Gen.identifier)

    "serialise to xdr" >> prop { bs: Array[Byte] =>
      val memo = MemoHash(bs.take(32))
      val xdr = memo.toXDR
      xdr.getDiscriminant mustEqual MEMO_HASH
      xdr.getHash.getHash.toSeq mustEqual paddedByteArray(bs.take(32), 32).toSeq
    }

    "be created from a hash" >> prop { bs: Array[Byte] =>
      val hex = bs.take(32).map("%02X".format(_)).mkString
      MemoHash.from(hex) must beSuccessfulTry.like { case m: MemoHash =>
        m.bs.toSeq mustEqual bs.take(32).toSeq
      }
      MemoHash.from(s"${hex}Z") must beFailedTry[MemoHash]
      (MemoHash.from(bs.map("%02X".format(_)).mkString) must beFailedTry[MemoHash]).unless(bs.length <= 32)
    }
  }

  "a memo return hash" should {
    "not be constructable with > 32 bytes" >> {
      MemoReturnHash((1 to 33).map(_.toByte).toArray) must throwAn[AssertionError]
    }

    "pad the input bytes" >> prop { bs: Array[Byte] =>
      MemoReturnHash(bs.take(32)).bytes mustEqual paddedByteArray(bs.take(32), 32)
    }

    "provide padded hex value" >> prop { s: String =>
      val hex = MemoReturnHash(s.take(32).getBytes("UTF-8")).hex
      new String(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte), "UTF-8").trim mustEqual s.take(32)
      hex.length mustEqual 64
    }.setGen(Gen.identifier)

    "provide trimmed hex value" >> prop { s: String =>
      val hex = MemoReturnHash(s.take(32).getBytes("UTF-8")).hexTrim
      new String(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte), "UTF-8") mustEqual s.take(32)
    }.setGen(Gen.identifier)

    "serialise to xdr" >> prop { bs: Array[Byte] =>
      val memo = MemoReturnHash(bs.take(32))
      val xdr = memo.toXDR
      xdr.getDiscriminant mustEqual MEMO_RETURN
      xdr.getHash.getHash.toSeq mustEqual paddedByteArray(bs.take(32), 32).toSeq
    }

    "be created from a hash" >> prop { bs: Array[Byte] =>
      val hex = bs.take(32).map("%02X".format(_)).mkString
      MemoReturnHash.from(hex) must beSuccessfulTry.like { case m: MemoReturnHash =>
        m.bs.toSeq mustEqual bs.take(32).toSeq
      }
      MemoReturnHash.from(s"${hex}Z") must beFailedTry[MemoReturnHash]
      (MemoReturnHash.from(bs.map("%02X".format(_)).mkString) must beFailedTry[MemoReturnHash]).unless(bs.length <= 32)
    }
  }

}

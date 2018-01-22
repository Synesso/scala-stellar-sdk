package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.MemoType.MEMO_HASH

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
  }

}

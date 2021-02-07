package stellar.sdk.model

import okio.ByteString
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk.util.ByteArrays._
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class MemoSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a text memo" should {
    "not be constructable with > 28 bytes" >> prop { s: String =>
      MemoText(s) must throwAn[AssertionError]
    }.setGen(arbString.arbitrary.suchThat(_.getBytes("UTF-8").length > 28))
  }

  "a id memo" should {
    "be constructable with zero" >> {
      MemoId(0) must not(throwAn[AssertionError])
    }

    "be constructable with negative number" >> prop { id: Long =>
      MemoId(id).toString mustEqual s"MemoId(${java.lang.Long.toUnsignedString(id)})"
    }.setGen(Gen.negNum[Long])
  }

  "a memo hash" should {
    "not be constructable with != 32 bytes" >> {
      MemoHash((1 to 33).map(_.toByte).toArray) must throwAn[AssertionError]
    }

    "be created from a hash" >> prop { hash64: String =>
      val hex = ByteString.decodeBase64(hash64).hex()
      MemoHash.from(hex) must beSuccessfulTry.like { case m: MemoHash =>
        m.bs.hex() mustEqual hex
      }
      MemoHash.from(s"${hex}Z") must beFailedTry[MemoHash]
    }.setGen(genHash)
  }

  "a memo return hash" should {
    "not be constructable with > 32 bytes" >> {
      MemoReturnHash((1 to 33).map(_.toByte).toArray) must throwAn[AssertionError]
    }

    "be created from a hash" >> prop { hash64: String =>
      val hex = ByteString.decodeBase64(hash64).hex()
      MemoReturnHash.from(hex) must beSuccessfulTry.like { case m: MemoReturnHash =>
        m.bs.hex() mustEqual hex
      }
      MemoReturnHash.from(s"${hex}Z") must beFailedTry[MemoReturnHash]
    }.setGen(genHash)
  }
}

package stellar.sdk.xdr

import java.nio.charset.Charset

import cats.data.State
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class SerdeSpec extends Specification with ScalaCheck {

  "round trip serialisation" should {
    "work for ints" >> prop { i: Int =>
      val (remainder, result) = Decode.int.run(Encode.int(i)).value
      remainder must beEmpty
      result mustEqual i
    }

    "work for longs" >> prop { l: Long =>
      val (remainder, result) = Decode.long.run(Encode.long(l)).value
      remainder must beEmpty
      result mustEqual l
    }

    "work for false" >> {
      val (remainder, result) = Decode.bool.run(Encode.bool(false)).value
      remainder must beEmpty
      result must beFalse
    }

    "work for true" >> {
      val (remainder, result) = Decode.bool.run(Encode.bool(true)).value
      remainder must beEmpty
      result must beTrue
    }

    "work for opaque bytes" >> prop { bs: Seq[Byte] =>
      val (remainder, result) = Decode.bytes.run(Encode.bytes(bs)).value
      remainder must beEmpty
      result mustEqual bs
    }

    "work for strings" >> prop { s: String =>
      val bytes = Encode.string(s)
      val (remainder, result) = Decode.string.run(bytes).value
      bytes.length % 4 mustEqual 0
      remainder must beEmpty
      result mustEqual s
    }

    "work for optional ints" >> prop { i: Option[Int] =>
      val (remainder, result) = Decode.opt(Decode.int).run(Encode.optInt(i)).value
      remainder must beEmpty
      result mustEqual i
    }

    "work for optional longs" >> prop { l: Option[Long] =>
      val (remainder, result) = Decode.opt(Decode.long).run(Encode.optLong(l)).value
      remainder must beEmpty
      result mustEqual l
    }

    "work for optional strings" >> prop { s: Option[String] =>
      val (remainder, result) = Decode.opt(Decode.string).run(Encode.optString(s)).value
      remainder must beEmpty
      result mustEqual s
    }

    "work for a list of encodables" >> prop { xs: Seq[String] =>
      val (remainder, result) = Decode.arr(Decode.string).run(Encode.arrString(xs)).value
      remainder must beEmpty
      result mustEqual xs
    }

    "work for a composite of encodables" >> prop { c: CompositeThing =>
      val (remainder, result) = CompositeThing.decode.run(c.encode).value
      remainder must beEmpty
      result mustEqual c
    }
  }

  "serialising xdr strings" should {
    "not pad with nulls if it is a multiple of 4" >> {
      Encode.string("1234") mustEqual Encode.int(4) ++ "1234".getBytes(Charset.forName("UTF-8"))
      Encode.string("12345678") mustEqual Encode.int(8) ++ "12345678".getBytes(Charset.forName("UTF-8"))
    }

    "pad with nulls if it is not a multiple of 4" >> {
      Encode.string("123") mustEqual Encode.int(3) ++ "123".getBytes(Charset.forName("UTF-8")) :+ 0
      Encode.string("12345") mustEqual Encode.int(5) ++ "12345".getBytes(Charset.forName("UTF-8")) ++ Seq(0, 0, 0)
      Encode.string("123456") mustEqual Encode.int(6) ++ "123456".getBytes(Charset.forName("UTF-8")) ++ Seq(0, 0)
      Encode.string("1234567") mustEqual Encode.int(7) ++ "1234567".getBytes(Charset.forName("UTF-8")) :+ 0
    }
  }

  implicit private val arbCompositeThing: Arbitrary[CompositeThing] = Arbitrary(genCompositeThing)
  private def genCompositeThing: Gen[CompositeThing] = for {
    b <- Gen.oneOf(true, false)
    s <- Gen.identifier
    bs <- Gen.option(Gen.containerOf[Seq, Byte](Gen.choose(0x00, 0xff).map(_.toByte)))
    ct <- Gen.option(genCompositeThing)
  } yield CompositeThing(b, s, bs, ct)

  case class CompositeThing(b: Boolean, s: String, bs: Option[Seq[Byte]], next: Option[CompositeThing]) extends Encodable {
    override def encode: Stream[Byte] = Encode.optBytes(bs) ++ Encode.bool(b) ++ Encode.opt(next) ++ Encode.string(s)
  }

  object CompositeThing {
    def decode: State[Seq[Byte], CompositeThing] = for {
      bs <- Decode.opt(Decode.bytes)
      b <- Decode.bool
      next <- Decode.opt[CompositeThing](CompositeThing.decode)
      s <- Decode.string
    } yield CompositeThing(b, s, bs, next)
  }


}

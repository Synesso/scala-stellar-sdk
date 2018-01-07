package stellar.scala.sdk

import org.specs2.mutable.Specification
import stellar.scala.sdk.StrKey._

class StrKeySpec extends Specification {

  "strkey" should {
    "encode and decode to same" >> {
      val seed = "SDJHRQF4GCMIIKAAAQ6IHY42X73FQFLHUULAPSKKD4DFDM7UXWWCRHBE"
      val roundTrip = encodeCheck(Seed, decodeCheck(Seed, seed.toCharArray)).mkString
      roundTrip mustEqual seed
    }

    "validate the version byte" >> {
      val address = "GCZHXL5HXQX5ABDM26LHYRCQZ5OJFHLOPLZX47WEBP3V2PF5AVFK2A5D"
      decodeCheck(Seed, address.toCharArray) must throwA[FormatException]
    }

    "fail to decode an invalid seed" >> {
      val seed = "SAA6NXOBOXP3RXGAXBW6PGFI5BPK4ODVAWITS4VDOMN5C2M4B66ZML"
      decodeCheck(Seed, seed.toCharArray) must throwA[FormatException]
    }
  }

}

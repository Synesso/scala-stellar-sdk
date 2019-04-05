package stellar.sdk.model

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class StrKeySpec extends Specification with ArbitraryInput with DomainMatchers {

  "strkey" should {
    "encode and decode to same" >> prop { key: StrKey =>
      StrKey.decodeFromString(key.encodeToChars.mkString) must beEquivalentTo(key)
    }

    "fail to decode if any char is > 127" >> {
      StrKey.decodeFromString("welcome to the 草叢") must throwAn[AssertionError]
    }
//    "validate the version byte" >> {
//      val address = "GCZHXL5HXQX5ABDM26LHYRCQZ5OJFHLOPLZX47WEBP3V2PF5AVFK2A5D"
//      decodeCheck(Seed, address.toCharArray) must throwA[FormatException]
//    }

//    "fail to decode an invalid seed" >> {
//      val seed = "SAA6NXOBOXP3RXGAXBW6PGFI5BPK4ODVAWITS4VDOMN5C2M4B66ZML"
//      decodeCheck(Seed, seed.toCharArray) must throwA[FormatException]
//    }

//    "fail decode seed with invalid chars" >> {
//      decodeStellarAccountId("GCZÅXL5HXQX5ABDM26LHYRCQZ5OJFHLOPLZX47WEBP3V2PF5AVFK2A5D") must
//        throwAn[IllegalArgumentException]
//    }
  }

}

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
  }

}

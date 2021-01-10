package stellar.sdk.model

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class SignerSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a signer" should {
    "serde via xdr bytes" >> prop { expected: Signer =>
      Signer.decode(expected.xdr) must beEquivalentTo(expected)
    }
  }

}

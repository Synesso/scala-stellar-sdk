package stellar.sdk.model

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class PaymentSigningRequestSpec extends Specification with ArbitraryInput with DomainMatchers {

  "encoding as a web+stellar url" should {
    "decode to the original" >> prop { signingRequest: PaymentSigningRequest =>
      val url = signingRequest.toUrl
      PaymentSigningRequest(url) must beEquivalentTo(signingRequest)
    }
  }

}

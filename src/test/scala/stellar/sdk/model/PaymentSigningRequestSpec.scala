package stellar.sdk.model

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class PaymentSigningRequestSpec extends Specification with ArbitraryInput with DomainMatchers {

  "encoding as a web+stellar url" should {
    "decode to the original" >> prop { signingRequest: PaymentSigningRequest =>
      val url = signingRequest.toUrl
      PaymentSigningRequest(url) mustEqual signingRequest
    }
  }

  "parsing from url" should {
    "fail when msg is greater than 300 chars" >> {
      val request: String = sampleOne(genPaymentSigningRequest).copy(message = None).toUrl
      PaymentSigningRequest(s"$request&msg=${"x" * 301}") must throwAn[IllegalArgumentException]
    }
  }
}

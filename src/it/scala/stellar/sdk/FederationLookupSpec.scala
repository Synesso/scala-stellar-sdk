package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.response.FederationResponse

import scala.concurrent.duration._

class FederationLookupSpec(implicit ec: ExecutionEnv) extends Specification {

  "federation server lookup" should {
    "find result by name" >> {
      FederationServer("https://keybase.io/_/api/1.0/stellar/federation.json")
        .byName("jem*keybase.io") must beSome(
        FederationResponse(
          address = "jem*keybase.io",
          account = KeyPair.fromAccountId("GBRAZP7U3SPHZ2FWOJLHPBO3XABZLKHNF6V5PUIJEEK6JEBKGXWD2IIE")
        )).awaitFor(10.seconds)
    }
  }

}

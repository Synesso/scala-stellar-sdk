package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.resp.AssetResp

import scala.concurrent.duration._

class AssetIntegrationSpec(implicit ee: ExecutionEnv) extends Specification {

  "asset endpoint" should {
    "list all assets" >> {
      val fifteen = TestNetwork.assets().map(_.take(15))
      fifteen.map(_.size) must beEqualTo(15).awaitFor(5 seconds)
      fifteen.map(_.last) must beEqualTo(
        AssetResp(AssetTypeCreditAlphaNum12("009688",
          KeyPair.fromAccountId("GCOGPF7IRVXUCJZAQWXVFQEE4HAOCTDGZI2QZSMKLM5BTTGRLY6GDOJN")),
          7340000000L,17,authRequired = false, authRevocable = false)
      ).awaitFor(5 seconds)
    }
  }
}

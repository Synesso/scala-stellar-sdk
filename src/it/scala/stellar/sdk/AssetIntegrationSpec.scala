package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.resp.AssetResp

import scala.concurrent.duration._

class AssetIntegrationSpec(implicit ee: ExecutionEnv) extends Specification {

  "asset endpoint" should {
    "list all assets" >> {
      val oneFifteen = TestNetwork.assets().map(_.take(115))
      oneFifteen.map(_.size) must beEqualTo(115).awaitFor(10 seconds)
      oneFifteen.map(_.last) must beEqualTo(
        AssetResp(AssetTypeCreditAlphaNum12("43a047",
          KeyPair.fromAccountId("GCOGPF7IRVXUCJZAQWXVFQEE4HAOCTDGZI2QZSMKLM5BTTGRLY6GDOJN")),
          7840000000L,18,authRequired = false, authRevocable = false)
      ).awaitFor(5 seconds)
    }

    "filter assets by code" >> {
      val byCode = TestNetwork.assets(code = Some("ALX1")).map(_.take(10).toList)
      byCode.foreach(_.foreach(println))
      byCode.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
      byCode.map(_.map(_.asset.code).toSet) must beEqualTo(Set("ALX1")).awaitFor(10 seconds)
    }
  }
}

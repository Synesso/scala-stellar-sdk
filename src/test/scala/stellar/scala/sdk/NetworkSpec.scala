package stellar.scala.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.scala.sdk.resp.FundTestAccountResponse
import scala.concurrent.duration._

class NetworkSpec(implicit ee: ExecutionEnv) extends Specification {

  "test network" should {
    "identify itself" >> {
      TestNetwork.passphrase mustEqual "Test SDF Network ; September 2015"
      BigInt(1, TestNetwork.networkId).toString(16).toUpperCase mustEqual
        "CEE0302D59844D32BDCA915C8203DD44B33FBB7EDC19051EA37ABEDF28ECD472"
    }

    "fund a new test account" >> {
      val kp = KeyPair.random
      TestNetwork.fund(kp) must beLike[FundTestAccountResponse] {
        case FundTestAccountResponse(a, b) =>
          // todo - check test network for confirmation of funding
          ok
      }.awaitFor(30.seconds)
    }
  }

  "public network" should {
    "identify itself" >> {
      PublicNetwork.passphrase mustEqual "Public Global Stellar Network ; September 2015"
      BigInt(1, PublicNetwork.networkId).toString(16).toUpperCase mustEqual
        "7AC33997544E3175D266BD022439B22CDB16508C01163F26E5CB2A3E1045A979"
    }
  }

}

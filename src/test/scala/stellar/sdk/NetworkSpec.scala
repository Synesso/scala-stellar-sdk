package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.resp.{AccountResp, FundTestAccountResponse}

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
      val fundedAccount = for {
        fundTestAccountResponse <- TestNetwork.fund(kp)
        accDetails <- TestNetwork.account(kp)
      } yield accDetails

      fundedAccount must beLike[AccountResp] {
        case AccountResp(id, _, _, _, _, _, List(lumens), _) =>
          id mustEqual kp.accountId
          lumens mustEqual Amount.lumens(10000).get
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

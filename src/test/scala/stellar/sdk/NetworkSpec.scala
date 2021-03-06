package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.duration._

class NetworkSpec(implicit ee: ExecutionEnv) extends Specification with ArbitraryInput with Mockito {

  "test network" should {
    "identify itself" >> {
      TestNetwork.passphrase mustEqual "Test SDF Network ; September 2015"
      BigInt(1, TestNetwork.networkId).toString(16).toUpperCase mustEqual
        "CEE0302D59844D32BDCA915C8203DD44B33FBB7EDC19051EA37ABEDF28ECD472"
    }

    "provide network info" >> {
      TestNetwork.info() must not(throwAn[Exception]).awaitFor(10.seconds)
    }
  }

  "public network" should {
    "identify itself" >> {
      PublicNetwork.passphrase mustEqual "Public Global Stellar Network ; September 2015"
      BigInt(1, PublicNetwork.networkId).toString(16).toUpperCase mustEqual
        "7AC33997544E3175D266BD022439B22CDB16508C01163F26E5CB2A3E1045A979"
    }
  }

  "any network" should {
    "provide access to the master account" >> {
      val networkAccountId = "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H"
      TestNetwork.masterAccount.accountId mustEqual networkAccountId
      KeyPair.fromSecretSeed(TestNetwork.masterAccount.secretSeed).accountId  mustEqual networkAccountId
    }
  }
}

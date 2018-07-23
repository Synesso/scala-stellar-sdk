package stellar.sdk

import org.json4s.CustomSerializer
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import stellar.sdk.inet.{HorizonAccess, Page}
import stellar.sdk.resp.{AccountResp, DataValueResp, TransactionPostResp}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class NetworkSpec(implicit ee: ExecutionEnv) extends Specification with ArbitraryInput with Mockito {

  "test network" should {
    "identify itself" >> {
      TestNetwork.passphrase mustEqual "Test SDF Network ; September 2015"
      BigInt(1, TestNetwork.networkId).toString(16).toUpperCase mustEqual
        "CEE0302D59844D32BDCA915C8203DD44B33FBB7EDC19051EA37ABEDF28ECD472"
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
    "submit a signed transaction" >> prop { txn: SignedTransaction =>
      val network = new MockNetwork
      val expected = Future(TransactionPostResp("hash", 1L, "envelopeXDR", "resultXDR", "resultMetaXDR"))
      network.horizon.post(txn) returns expected
      network.submit(txn) mustEqual expected
    }

    "fetch account details by account id" >> prop { pk: PublicKey =>
      val network = new MockNetwork
      val response = mock[AccountResp]
      val expected = Future(response)
      network.horizon.get[AccountResp](s"/accounts/${pk.accountId}") returns expected
      network.account(pk) mustEqual expected
    }

    "fetch data for an account by account id and key" >> prop { (pk: PublicKey, key: String, value: String) =>
      val network = new MockNetwork
      val response = DataValueResp(ByteArrays.base64(value.getBytes("UTF-8")))
      val expected = Future(response)
      network.horizon.get[DataValueResp](s"/accounts/${pk.accountId}/data/$key") returns expected
      network.accountData(pk, key) must beEqualTo(value).await
    }
  }

  class MockNetwork extends Network {
    override val passphrase: String = "Scala SDK mock network"
    override val horizon: HorizonAccess = mock[HorizonAccess]
  }
}


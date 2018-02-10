package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.SessionTestAccount.{accWithData, accn}
import stellar.sdk.inet.ResourceMissingException
import stellar.sdk.resp.{AccountResp, AssetResp}

import scala.concurrent.duration._

class SequentialIntegrationSpec(implicit ee: ExecutionEnv) extends Specification with DomainMatchersIT {

  sequential

  "account endpoint" >> {
    "fetch account details" >> {
      TestNetwork.account(accn) must beLike[AccountResp] {
        case AccountResp(id, _, _, _, _, _, List(lumens), _) =>
          id mustEqual accn.accountId
          lumens mustEqual Amount.lumens(10000).get
          // todo - add check for data when we can submit manage data ops
      }.awaitFor(30.seconds)
    }

    "fetch nothing if no account exists" >> {
      TestNetwork.account(KeyPair.random) must throwA[ResourceMissingException].awaitFor(5.seconds)
    }

    "return the data for an account" >> {
      TestNetwork.accountData(accWithData, "life_universe_everything") must beEqualTo("42").awaitFor(5.seconds)
    }

    "fetch nothing if no data exists for the account" >> {
      TestNetwork.accountData(accWithData, "brain_size_of_planet") must throwA[ResourceMissingException].awaitFor(5.seconds)
    }
  }

  "asset endpoint" should {
    "list all assets" >> {
      val oneFifteen = TestNetwork.assets().map(_.take(115))
      oneFifteen.map(_.distinct.size) must beEqualTo(115).awaitFor(10 seconds)
    }

    "filter assets by code" >> {
      val byCode = TestNetwork.assets(code = Some("ALX1")).map(_.take(10).toList)
      byCode.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
      byCode.map(_.map(_.asset.code).toSet) must beEqualTo(Set("ALX1")).awaitFor(10 seconds)
    }

    "filter assets by issuer" >> {
      val issuerAccount = "GCZAKXMQZKYJBQK7U2LFIF77PKGDCZRU3IOPV2VON5CHWJSWDH2B5A42"
      val byIssuer = TestNetwork.assets(issuer = Some(issuerAccount)).map(_.take(10).toList)
      byIssuer.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
      byIssuer.map(_.map(_.asset.issuer.accountId).toSet) must beEqualTo(Set(issuerAccount)).awaitFor(10 seconds)
    }

    "filter assets by code and issuer" >> {
      val issuerAccount = "GCZAKXMQZKYJBQK7U2LFIF77PKGDCZRU3IOPV2VON5CHWJSWDH2B5A42"
      val byCodeAndIssuer = TestNetwork.assets(code = Some("ALX1"), issuer = Some(issuerAccount)).map(_.toList)
      byCodeAndIssuer.map(_.map(_.asset)) must beLike[Seq[NonNativeAsset]] {
        case Seq(asset) => asset must beEquivalentTo(AssetTypeCreditAlphaNum4("ALX1", KeyPair.fromAccountId(issuerAccount)))
      }.awaitFor(10 seconds)
    }
  }



}

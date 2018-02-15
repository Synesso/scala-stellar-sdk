package stellar.sdk

import java.time.ZonedDateTime

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.SessionTestAccount.{accWithData, accn}
import stellar.sdk.inet.ResourceMissingException
import stellar.sdk.resp._

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

  "effect endpoint" should {
    "list all effects" >> {
      val oneFifteen = TestNetwork.effects().map(_.take(115))
      oneFifteen.map(_.distinct.size) must beEqualTo(115).awaitFor(10.seconds)
    }

    "filter effects by account" >> {
      val byAccount = TestNetwork.effectsByAccount(accn).map(_.take(10).toList)
      byAccount.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
      byAccount.map(_.head) must beLike[EffectResp] {
        case EffectAccountCreated(_, account, startingBalance) =>
          account.accountId mustEqual accn.accountId
          startingBalance mustEqual Amount.lumens(10000).get
      }.awaitFor(10.seconds)
    }

    "filter effects by ledger" >> {
      val byLedger = PublicNetwork.effectsByLedger(16237465).map(_.toList)
      byLedger must beEqualTo(Seq(
        EffectTrade("0069739381144948737-0000000001", 747605, KeyPair.fromAccountId("GD3IYBNQ45LXHFABSX4HLGDL7BQA62SVB5NB5O6XMBCITFZOLWLVS22B"),
          Amount(5484522, AssetTypeCreditAlphaNum4("XLM", KeyPair.fromAccountId("GBSTRH4QOTWNSVA6E4HFERETX4ZLSR3CIUBLK7AXYII277PFJC4BBYOG"))),
          KeyPair.fromAccountId("GBBMSYSNV7PC6XAI3JL6F5OWP54TIONGDDTEJ4AQS3YMFUSPDSSSDQVB"),
          Amount(2445, AssetTypeCreditAlphaNum4("ETH", KeyPair.fromAccountId("GBSTRH4QOTWNSVA6E4HFERETX4ZLSR3CIUBLK7AXYII277PFJC4BBYOG")))),

        EffectTrade("0069739381144948737-0000000002", 747605, KeyPair.fromAccountId("GBBMSYSNV7PC6XAI3JL6F5OWP54TIONGDDTEJ4AQS3YMFUSPDSSSDQVB"),
          Amount(2445, AssetTypeCreditAlphaNum4("ETH", KeyPair.fromAccountId("GBSTRH4QOTWNSVA6E4HFERETX4ZLSR3CIUBLK7AXYII277PFJC4BBYOG"))),
          KeyPair.fromAccountId("GD3IYBNQ45LXHFABSX4HLGDL7BQA62SVB5NB5O6XMBCITFZOLWLVS22B"),
          Amount(5484522, AssetTypeCreditAlphaNum4("XLM", KeyPair.fromAccountId("GBSTRH4QOTWNSVA6E4HFERETX4ZLSR3CIUBLK7AXYII277PFJC4BBYOG")))
        )
      )).awaitFor(10.seconds)
    }
  }

  "ledger endpoint" should {
    "list the details of a given ledger" >> {
      PublicNetwork.ledger(16237465) must beEqualTo(
        LedgerResp("d4a8dae64397e23551a5b57e30ae16d6887b6a49fb9263808878fd6dc71f64be",
          "d4a8dae64397e23551a5b57e30ae16d6887b6a49fb9263808878fd6dc71f64be",
          Some("ec7d2a4c064a1f10741b93260fc5b625febdf8cc5a06df8a892396ecab4449d0"), 16237465L, 1, 1,
          ZonedDateTime.parse("2018-02-13T00:33:53Z"), 1.0368912397155042E11, 1415204.6354335, 100, 0.5, 50)
      ).awaitFor(10.seconds)
    }

    "list all ledgers" >> {
      val oneFifteen = TestNetwork.ledgers().map(_.take(115))
      oneFifteen.map(_.distinct.size) must beEqualTo(115).awaitFor(10.seconds)
    }
  }

  "offer endpoint" should {
    "list offers by account" >> {
      TestNetwork.offersByAccount(KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"))
        .map(_.toSeq) must beEqualTo(Seq(
        OfferResp(
          id = 101542,
          seller = KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"),
          selling = Amount.lumens(165).get,
          buying = AssetTypeCreditAlphaNum12(
            code = "sausage",
            issuer = KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK")),
          price = Price(303, 100)
        )
      )).awaitFor(10.seconds)
    }
  }

}

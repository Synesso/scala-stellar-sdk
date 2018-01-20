package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.{AccountID, AllowTrustOp, AssetType}
import stellar.scala.sdk._

class AllowTrustOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "allow trust operation" should {
    "serde via xdr" >> prop { (source: KeyPair, trustor: VerifyingKey, asset: NonNativeAsset, authorize: Boolean) =>
      val input = AllowTrustOperation(source, trustor, asset.code, authorize)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case ato: AllowTrustOperation =>
          ato.trustor must beEquivalentTo(trustor)
          ato.authorize mustEqual authorize
          ato.assetCode mustEqual asset.code
          ato.sourceAccount must beNone
      }
    }

    "should not have native asset type" >> {
      val input = new AllowTrustOp
      input.setAsset(new AllowTrustOp.AllowTrustOpAsset)
      input.getAsset.setDiscriminant(AssetType.ASSET_TYPE_NATIVE)
      input.setTrustor(new AccountID)
      input.getTrustor.setAccountID(KeyPair.random.getXDRPublicKey)
      AllowTrustOperation.from(input) must beFailedTry[AllowTrustOperation]
    }
  }

}

package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.{AccountID, AllowTrustOp, AssetType}
import stellar.scala.sdk._

class AllowTrustOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "allow trust operation" should {
    "serde via xdr" >> prop { actual: AllowTrustOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: AllowTrustOperation => expected must beEquivalentTo(actual)
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

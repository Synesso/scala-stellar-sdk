package stellar.sdk.model

import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.ClaimantGenerators.genClaimant

class ClaimantSpec extends Specification with ScalaCheck {

  implicit val arbClaimant: Arbitrary[Claimant] = Arbitrary(genClaimant)

  "a claimant" should {
    "serde to/from XDR" >> prop { claimant: Claimant =>
      val (state, decoded) = Claimant.decode.run(claimant.encode).value
      state.isEmpty must beTrue
      decoded mustEqual claimant
    }
  }
}

object ClaimantGenerators extends ArbitraryInput {

  val genClaimant: Gen[Claimant] = for {
    accountId <- genPublicKey
    predicate <- ClaimPredicateGenerators.genClaimPredicate
  } yield AccountIdClaimant(accountId, predicate)

}
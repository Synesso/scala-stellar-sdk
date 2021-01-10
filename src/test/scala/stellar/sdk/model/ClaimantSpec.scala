package stellar.sdk.model

import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.json4s.{Formats, NoTypeHints}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.ClaimantGenerators.{genAccountIdClaimant, genClaimant, json}

class ClaimantSpec extends Specification with ScalaCheck {

  implicit val arbClaimant: Arbitrary[Claimant] = Arbitrary(genClaimant)
  implicit val arbAccountIdClaimant: Arbitrary[AccountIdClaimant] = Arbitrary(genAccountIdClaimant)
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + ClaimantDeserializer

  "a claimant" should {
    "serde to/from XDR" >> prop { claimant: Claimant =>
      Claimant.decode(claimant.xdr) mustEqual claimant
    }

    "parse from JSON" >> prop { claimant: AccountIdClaimant =>
      parse(json(claimant)).extract[Claimant] mustEqual claimant
    }.set(minTestsOk = 10000)
  }
}

object ClaimantGenerators extends ArbitraryInput {

  val genAccountIdClaimant: Gen[AccountIdClaimant] = for {
    accountId <- genPublicKey
    predicate <- ClaimPredicateGenerators.genClaimPredicate
  } yield AccountIdClaimant(accountId, predicate)

  val genClaimant: Gen[Claimant] = Gen.oneOf(genAccountIdClaimant, genAccountIdClaimant)

  def json(claimant: Claimant): String = claimant match {
    case a: AccountIdClaimant => json(a)
  }

  def json(claimant: AccountIdClaimant): String =
    s"""
       |{
       |  "destination": "${claimant.accountId.accountId}",
       |  "predicate": ${ClaimPredicateGenerators.json(claimant.predicate)}
       |}""".stripMargin

}
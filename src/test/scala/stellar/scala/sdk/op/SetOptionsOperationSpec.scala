package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk.{ArbitraryInput, DomainMatchers}

class SetOptionsOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "set options operation" should {
    "serde via xdr" >> prop { expected: SetOptionsOperation =>
      Operation.fromXDR(expected.toXDR) must beSuccessfulTry.like {
        case actual: SetOptionsOperation =>
          actual.homeDomain mustEqual expected.homeDomain
          actual.clearFlags mustEqual expected.clearFlags
          actual.setFlags mustEqual expected.setFlags
          actual.lowThreshold mustEqual expected.lowThreshold
          actual.mediumThreshold mustEqual expected.mediumThreshold
          actual.highThreshold mustEqual expected.highThreshold
          actual.masterKeyWeight mustEqual expected.masterKeyWeight
          actual.inflationDestination.map(_.accountId) mustEqual expected.inflationDestination.map(_.accountId)
          actual.signer must beLike {
            case None => expected.signer must beNone
            case Some((actualSK, actualWeight)) =>
              expected.signer must beSome.like { case (expectedSK, expectedWeight) =>
                actualSK must beEquivalentTo(expectedSK)
                actualWeight mustEqual expectedWeight
              }
          }
          actual.sourceAccount must beNone
      }
    }
  }
}

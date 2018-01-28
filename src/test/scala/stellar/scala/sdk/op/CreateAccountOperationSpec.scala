package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk._

class CreateAccountOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "create account operation" should {
    "serde via xdr" >> prop { actual: CreateAccountOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: CreateAccountOperation => expected must beEquivalentTo(actual)
      }
    }
  }

}

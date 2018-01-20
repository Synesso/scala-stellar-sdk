package stellar.scala.sdk.op

import org.specs2.mutable.Specification
import stellar.scala.sdk._

class CreatePassiveOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "create passive offer operation" should {
    "serde via xdr" >> prop { (selling: Amount, buying: Asset, price: Price, source: Option[KeyPair]) =>
      val input = CreatePassiveOfferOperation(selling, buying, price, source)
      val triedOperation = Operation.fromXDR(input.toXDR)
      if (triedOperation.isFailure) throw triedOperation.failed.get
      triedOperation must beSuccessfulTry.like {
        case co: CreatePassiveOfferOperation =>
          co.selling must beEquivalentTo(selling)
          co.buying must beEquivalentTo(buying)
          co.price mustEqual price
          co.sourceAccount must beNone
      }
    }
  }

}

package stellar.scala.sdk.op

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.ManageOfferOp
import stellar.scala.sdk._

class ManageOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "create offer operation" should {
    "serde via xdr" >> prop {
      (source: KeyPair, selling: Amount, buying: Asset, price: Price) =>
        val input = CreateOfferOperation(selling, buying, price, Some(source))
        val triedOperation = Operation.fromXDR(input.toXDR)
        if (triedOperation.isFailure) throw triedOperation.failed.get
        triedOperation must beSuccessfulTry.like {
          case co: CreateOfferOperation =>
            co.selling must beEquivalentTo(selling)
            co.buying must beEquivalentTo(buying)
            co.price mustEqual price
            co.sourceAccount must beNone
            co.offerId mustEqual 0
        }
    }
  }

  "update offer operation" should {
    "serde via xdr" >> prop {
      (source: KeyPair, offerId: Long, selling: Amount, buying: Asset, price: Price) =>
        val input = UpdateOfferOperation(offerId, selling, buying, price, Some(source))
        val triedOperation = Operation.fromXDR(input.toXDR)
        if (triedOperation.isFailure) throw triedOperation.failed.get
        triedOperation must beSuccessfulTry.like {
          case uoo: UpdateOfferOperation =>
            uoo.offerId mustEqual offerId
            uoo.selling must beEquivalentTo(selling)
            uoo.buying must beEquivalentTo(buying)
            uoo.price mustEqual price
            uoo.sourceAccount must beNone
        }
    }.setGen2(Gen.posNum[Long])
  }

  "delete offer operation" should {
    "serde via xdr" >> prop {
      (source: KeyPair, offerId: Long, selling: Asset, buying: Asset, price: Price) =>
        val input = DeleteOfferOperation(offerId, selling, buying, price, Some(source))
        val triedOperation = Operation.fromXDR(input.toXDR)
        if (triedOperation.isFailure) throw triedOperation.failed.get
        triedOperation must beSuccessfulTry.like {
          case doo: DeleteOfferOperation =>
            doo.offerId mustEqual offerId
            doo.selling must beEquivalentTo(selling)
            doo.buying must beEquivalentTo(buying)
            doo.price mustEqual price
            doo.sourceAccount must beNone
        }
    }.setGen2(Gen.posNum[Long])
  }

  "manage offer op with no id and no details" should {
    "not deserialise" >> {
      ManageOfferOperation.from(new ManageOfferOp) must beFailedTry[ManageOfferOperation]
    }
  }

}

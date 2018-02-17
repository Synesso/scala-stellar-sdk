package stellar.sdk.op

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.JsonMethods.parse
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.ManageOfferOp
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class ManageOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arbCreate: Arbitrary[Transacted[CreateOfferOperation]] = Arbitrary(genTransacted(genCreateOfferOperation))
  implicit val arbUpdate: Arbitrary[Transacted[UpdateOfferOperation]] = Arbitrary(genTransacted(genUpdateOfferOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer + OperationDeserializer

  "create offer operation" should {
    "serde via xdr" >> prop { actual: CreateOfferOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: CreateOfferOperation => expected must beEquivalentTo(actual)
      }
    }

    "be parsed from json" >> prop { op: Transacted[CreateOfferOperation] =>
      val doc =
        s"""
          |{
          |  "_links":{
          |    "self":{"href":"https://horizon.stellar.org/operations/109521666052097"},
          |    "transaction":{"href":"https://horizon.stellar.org/transactions/85bcf02eed66ac86fd3d26e813214f2d4e25841d8420f7f52d377418040f4592"},
          |    "effects":{"href":"https://horizon.stellar.org/operations/109521666052097/effects"},
          |    "succeeds":{"href":"https://horizon.stellar.org/effects?order=desc&cursor=109521666052097"},
          |    "precedes":{"href":"https://horizon.stellar.org/effects?order=asc&cursor=109521666052097"}
          |  },
          |  "id":"${op.id}",
          |  "paging_token":"109521666052097",
          |  "source_account":"${op.sourceAccount.accountId}",
          |  "type":"manage_offer",
          |  "type_i":3,
          |  "created_at":"${formatter.format(op.createdAt)}",
          |  "transaction_hash":"${op.txnHash}",
          |  ${amountDocPortion(op.operation.selling, assetPrefix = "selling_")}
          |  ${asset(op.operation.buying, assetPrefix = "buying_")}
          |  "price":"1.0000000",
          |  "price_r":{
          |    "n":${op.operation.price.n},
          |    "d":${op.operation.price.d}
          |  },
          |  "offer_id":0
          |}
        """.stripMargin

      parse(doc).extract[Transacted[Operation]] mustEqual op
    }
  }

  "update offer operation" should {
    "serde via xdr" >> prop { actual: UpdateOfferOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: UpdateOfferOperation => expected must beEquivalentTo(actual)
      }
    }

    "be parsed from json" >> prop { op: Transacted[UpdateOfferOperation] =>
      val doc =
        s"""
           |{
           |  "_links":{
           |    "self":{"href":"https://horizon.stellar.org/operations/109521666052097"},
           |    "transaction":{"href":"https://horizon.stellar.org/transactions/85bcf02eed66ac86fd3d26e813214f2d4e25841d8420f7f52d377418040f4592"},
           |    "effects":{"href":"https://horizon.stellar.org/operations/109521666052097/effects"},
           |    "succeeds":{"href":"https://horizon.stellar.org/effects?order=desc&cursor=109521666052097"},
           |    "precedes":{"href":"https://horizon.stellar.org/effects?order=asc&cursor=109521666052097"}
           |  },
           |  "id":"${op.id}",
           |  "paging_token":"109521666052097",
           |  "source_account":"${op.sourceAccount.accountId}",
           |  "type":"manage_offer",
           |  "type_i":3,
           |  "created_at":"${formatter.format(op.createdAt)}",
           |  "transaction_hash":"${op.txnHash}",
           |  ${amountDocPortion(op.operation.selling, assetPrefix = "selling_")}
           |  ${asset(op.operation.buying, assetPrefix = "buying_")}
           |  "price":"1.0000000",
           |  "price_r":{
           |    "n":${op.operation.price.n},
           |    "d":${op.operation.price.d}
           |  },
           |  "offer_id":${op.operation.offerId}
           |}
        """.stripMargin

      parse(doc).extract[Transacted[Operation]] mustEqual op
    }
  }

  "delete offer operation" should {
    "serde via xdr" >> prop { actual: DeleteOfferOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: DeleteOfferOperation => expected must beEquivalentTo(actual)
      }
    }
  }

  "manage offer op with no id and no details" should {
    "not deserialise" >> {
      ManageOfferOperation.from(new ManageOfferOp) must beFailedTry[ManageOfferOperation]
    }
  }

}

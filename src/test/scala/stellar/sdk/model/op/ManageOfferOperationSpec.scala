package stellar.sdk.model.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.util.ByteArrays.base64
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class ManageOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arbCreate: Arbitrary[Transacted[CreateOfferOperation]] = Arbitrary(genTransacted(genCreateOfferOperation))
  implicit val arbDelete: Arbitrary[Transacted[DeleteOfferOperation]] = Arbitrary(genTransacted(genDeleteOfferOperation))
  implicit val arbUpdate: Arbitrary[Transacted[UpdateOfferOperation]] = Arbitrary(genTransacted(genUpdateOfferOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer + OperationDeserializer

  "create offer operation" should {
    "serde via xdr string" >> prop { actual: CreateOfferOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: CreateOfferOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
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
           |  "source_account":"${op.operation.sourceAccount.get.accountId}",
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
    }.setGen(genTransacted(genCreateOfferOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

  "update offer operation" should {
    "serde via xdr string" >> prop { actual: UpdateOfferOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: UpdateOfferOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
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
           |  "source_account":"${op.operation.sourceAccount.get.accountId}",
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
    }.setGen(genTransacted(genUpdateOfferOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

  "delete offer operation" should {
    "serde via xdr string" >> prop { actual: DeleteOfferOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: DeleteOfferOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "be parsed from json" >> prop { op: Transacted[DeleteOfferOperation] =>
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
           |  "source_account":"${op.operation.sourceAccount.get.accountId}",
           |  "type":"manage_offer",
           |  "type_i":3,
           |  "created_at":"${formatter.format(op.createdAt)}",
           |  "transaction_hash":"${op.txnHash}",
           |  "amount":"0.0000000",
           |  ${asset(op.operation.selling, assetPrefix = "selling_")}
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
    }.setGen(genTransacted(genDeleteOfferOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

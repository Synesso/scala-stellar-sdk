package stellar.sdk.model.op

import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.json4s.{Formats, NoTypeHints}
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class ManageSellOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arbCreate: Arbitrary[Transacted[CreateSellOfferOperation]] = Arbitrary(genTransacted(genCreateSellOfferOperation))
  implicit val arbDelete: Arbitrary[Transacted[DeleteSellOfferOperation]] = Arbitrary(genTransacted(genDeleteSellOfferOperation))
  implicit val arbUpdate: Arbitrary[Transacted[UpdateSellOfferOperation]] = Arbitrary(genTransacted(genUpdateSellOfferOperation))
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer + OperationDeserializer

  "create sell offer operation" should {
    "serde via xdr string" >> prop { actual: CreateSellOfferOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: CreateSellOfferOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "be parsed from json" >> prop { op: Transacted[CreateSellOfferOperation] =>
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
           |  ${accountId(op.operation.sourceAccount.get, "source_account")}
           |  "type":"manage_sell_offer",
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
    }.setGen(genTransacted(genCreateSellOfferOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

  "update sell offer operation" should {
    "serde via xdr string" >> prop { actual: UpdateSellOfferOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: UpdateSellOfferOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "be parsed from json" >> prop { op: Transacted[UpdateSellOfferOperation] =>
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
           |  ${accountId(op.operation.sourceAccount.get, "source_account")}
           |  "type":"manage_sell_offer",
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
    }.setGen(genTransacted(genUpdateSellOfferOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

  "delete sell offer operation" should {
    "serde via xdr string" >> prop { actual: DeleteSellOfferOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: DeleteSellOfferOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "be parsed from json" >> prop { op: Transacted[DeleteSellOfferOperation] =>
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
           |  ${accountId(op.operation.sourceAccount.get, "source_account")}
           |  "type":"manage_sell_offer",
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
    }.setGen(genTransacted(genDeleteSellOfferOperation.suchThat(_.sourceAccount.nonEmpty)))

    "be parsed from json with v0.18.0 transaction type" >> prop { op: Transacted[DeleteSellOfferOperation] =>
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
           |  ${accountId(op.operation.sourceAccount.get, "source_account")}
           |  "type":"manage_sell_offer",
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
    }.setGen(genTransacted(genDeleteSellOfferOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

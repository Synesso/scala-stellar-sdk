package stellar.sdk.model.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.util.ByteArrays.base64
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class CreatePassiveSellOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[CreatePassiveSellOfferOperation]] = Arbitrary(genTransacted(genCreatePassiveSellOfferOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer + OperationDeserializer

  "create passive offer operation" should {
    "serde via xdr string" >> prop { actual: CreatePassiveSellOfferOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: CreatePassiveSellOfferOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "parse from json" >> prop { op: Transacted[CreatePassiveSellOfferOperation] =>
      val doc =
        s"""
           |{
           |  "_links": {
           |    "self": {"href": "https://horizon-testnet.stellar.org/operations/10157597659137"},
           |    "transaction": {"href": "https://horizon-testnet.stellar.org/transactions/17a670bc424ff5ce3b386dbfaae9990b66a2a37b4fbe51547e8794962a3f9e6a"},
           |    "effects": {"href": "https://horizon-testnet.stellar.org/operations/10157597659137/effects"},
           |    "succeeds": {"href": "https://horizon-testnet.stellar.org/effects?order=desc\u0026cursor=10157597659137"},
           |    "precedes": {"href": "https://horizon-testnet.stellar.org/effects?order=asc\u0026cursor=10157597659137"}
           |  },
           |  "id": "${op.id}",
           |  "paging_token": "10157597659137",
           |  "source_account": "${op.operation.sourceAccount.get.accountId}",
           |  "type": "create_passive_sell_offer",
           |  "type_i": 4,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  ${amountDocPortion(op.operation.selling, assetPrefix = "selling_")},
           |  ${asset(op.operation.buying, "buying_")},
           |  "offer_id": 0,
           |  "price": "1.0",
           |  "price_r": {
           |    "d": ${op.operation.price.d},
           |    "n": ${op.operation.price.n}
           |  }
           |}
         """.stripMargin

      parse(doc).extract[Transacted[CreatePassiveSellOfferOperation]] mustEqual op
    }.setGen(genTransacted(genCreatePassiveSellOfferOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

package stellar.sdk.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ByteArrays.base64
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class CreatePassiveOfferOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[CreatePassiveOfferOperation]] = Arbitrary(genTransacted(genCreatePassiveOfferOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer + OperationDeserializer

  "create passive offer operation" should {
    "serde via xdr string" >> prop { actual: CreatePassiveOfferOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: CreatePassiveOfferOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "parse from json" >> prop { op: Transacted[CreatePassiveOfferOperation] =>
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
           |  "type": "create_passive_offer",
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

      parse(doc).extract[Transacted[CreatePassiveOfferOperation]] mustEqual op
    }.setGen(genTransacted(genCreatePassiveOfferOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

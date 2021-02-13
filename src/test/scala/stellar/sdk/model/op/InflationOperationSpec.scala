package stellar.sdk.model.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.util.ByteArrays.base64
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class InflationOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[InflationOperation]] = Arbitrary(genTransacted(genInflationOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "the inflation operation" should {
    "serde via xdr string" >> prop { actual: InflationOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) mustEqual actual
    }

    "serde via xdr bytes" >> prop { actual: InflationOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "parse from json" >> prop { op: Transacted[InflationOperation] =>
      val doc =
        s"""
           | {
           |  "_links": {
           |    "self": {"href": "https://horizon-testnet.stellar.org/operations/10157597659144"},
           |    "transaction": {"href": "https://horizon-testnet.stellar.org/transactions/17a670bc424ff5ce3b386dbfaae9990b66a2a37b4fbe51547e8794962a3f9e6a"},
           |    "effects": {"href": "https://horizon-testnet.stellar.org/operations/10157597659144/effects"},
           |    "succeeds": {"href": "https://horizon-testnet.stellar.org/effects?order=desc\u0026cursor=10157597659144"},
           |    "precedes": {"href": "https://horizon-testnet.stellar.org/effects?order=asc\u0026cursor=10157597659144"}
           |  },
           |  "id": "${op.id}",
           |  "paging_token": "10157597659137",
           |  "source_account": "${op.operation.sourceAccount.get.accountId}",
           |  "type": "inflation",
           |  "type_i": 9,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |}
         """.stripMargin

      parse(doc).extract[Transacted[InflationOperation]] mustEqual op
    }.setGen(genTransacted(genInflationOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

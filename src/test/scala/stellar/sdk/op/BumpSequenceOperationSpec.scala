package stellar.sdk.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, ByteArrays, DomainMatchers}

class BumpSequenceOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[BumpSequenceOperation]] = Arbitrary(genTransacted(genBumpSequenceOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "bump sequence operation" should {
    "serde via xdr bytes" >> prop { actual: BumpSequenceOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "serde via xdr string" >> prop { actual: BumpSequenceOperation =>
      Operation.decodeXDR(ByteArrays.base64(actual.encode)) mustEqual actual
    }

    "parse from json" >> prop { op: Transacted[BumpSequenceOperation] =>
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
           |  "type": "bump_sequence",
           |  "type_i": 11,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "bump_to": ${op.operation.bumpTo}
           |}
         """.stripMargin

      parse(doc).extract[Transacted[BumpSequenceOperation]] mustEqual op
    }.setGen(genTransacted(genBumpSequenceOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

package stellar.sdk.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ByteArrays.base64
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair}

class AllowTrustOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[AllowTrustOperation]] = Arbitrary(genTransacted(genAllowTrustOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "allow trust operation" should {
    "serde via xdr string" >> prop { actual: AllowTrustOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: AllowTrustOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "parse from json" >> prop { op: Transacted[AllowTrustOperation] =>
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
           |  "type": "allow_trust",
           |  "type_i": 7,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "asset_type": "${if (op.operation.assetCode.length <= 4) "credit_alphanum4" else "credit_alphanum12"}",
           |  "asset_code": "${op.operation.assetCode}",
           |  "asset_issuer": "${op.operation.sourceAccount.get.accountId}"
           |  "trustor": "${op.operation.trustor.accountId}",
           |  "trustee": "${op.operation.sourceAccount.get.accountId}",
           |  "authorize": ${op.operation.authorize}
           |}
         """.stripMargin

      parse(doc).extract[Transacted[AllowTrustOperation]] mustEqual op
    }.setGen(genTransacted(genAllowTrustOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

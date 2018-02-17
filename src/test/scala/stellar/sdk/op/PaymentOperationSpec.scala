package stellar.sdk.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.resp.OperationResp
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class PaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[PaymentOperation]] = Arbitrary(genTransacted(genPaymentOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "payment operation" should {
    "serde via xdr" >> prop { actual: PaymentOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: PaymentOperation => expected must beEquivalentTo(actual)
      }
    }

    "parse from json" >> prop { op: Transacted[PaymentOperation] =>
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
           |  "source_account": "${op.sourceAccount.accountId}",
           |  "type": "payment",
           |  "type_i": 1,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  ${amountDocPortion(op.operation.amount)},
           |  "from": "${op.sourceAccount.accountId}",
           |  "to": "${op.operation.destinationAccount.accountId}",
           |}
         """.stripMargin

      parse(doc).extract[Transacted[PaymentOperation]] mustEqual op

    }
  }

}

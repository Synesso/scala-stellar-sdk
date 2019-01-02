package stellar.sdk.model.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.util.ByteArrays.base64
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class CreateAccountOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[CreateAccountOperation]] = Arbitrary(genTransacted(genCreateAccountOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer + OperationDeserializer

  "create account operation" should {
    "serde via xdr string" >> prop { actual: CreateAccountOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: CreateAccountOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "be parsed from json " >> prop { op: Transacted[CreateAccountOperation] =>
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
           |  "type": "create_account",
           |  "type_i": 0,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "starting_balance": "${amountString(op.operation.startingBalance)}",
           |  "funder": "${op.operation.sourceAccount.get.accountId}",
           |  "account": "${op.operation.destinationAccount.accountId}"
           |}
         """.stripMargin

      parse(doc).extract[Transacted[CreateAccountOperation]] mustEqual op
    }.setGen(genTransacted(genCreateAccountOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

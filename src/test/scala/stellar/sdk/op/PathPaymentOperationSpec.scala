package stellar.sdk.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ByteArrays.base64
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class PathPaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[PathPaymentOperation]] = Arbitrary(genTransacted(genPathPaymentOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "path payment operation" should {
    "serde via xdr string" >> prop { actual: PathPaymentOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: PathPaymentOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "parse from json" >> prop { op: Transacted[PathPaymentOperation] =>
      val doc =
        s"""
           |{
           |  "_links":{
           |    "self":{"href":"https://horizon-testnet.stellar.org/operations/940258535411713"},
           |    "transaction":{"href":"https://horizon-testnet.stellar.org/transactions/a995af17837d1b53fb5782269250a36e9dbe74170260b46f2708e5f23f7c864a"},
           |    "effects":{"href":"https://horizon-testnet.stellar.org/operations/940258535411713/effects"},
           |    "succeeds":{"href":"https://horizon-testnet.stellar.org/effects?order=desc&cursor=940258535411713"},
           |    "precedes":{"href":"https://horizon-testnet.stellar.org/effects?order=asc&cursor=940258535411713"}
           |  },
           |  "id": "${op.id}",
           |  "paging_token": "10157597659137",
           |  "source_account": "${op.operation.sourceAccount.get.accountId}",
           |  "type":"path_payment",
           |  "type_i":2,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  ${amountDocPortion(op.operation.destinationAmount)}
           |  ${amountDocPortion(op.operation.sendMax, "source_max", "source_")}
           |  "from":"${op.operation.sourceAccount.get.accountId}",
           |  "to":"${op.operation.destinationAccount.accountId}",
           |  "path":[${if (op.operation.path.isEmpty) "" else op.operation.path.map(asset(_)).mkString("{", "},{", "}")}]
           |}
         """.stripMargin

      parse(doc).extract[Transacted[Operation]] mustEqual op
    }.setGen(genTransacted(genPathPaymentOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

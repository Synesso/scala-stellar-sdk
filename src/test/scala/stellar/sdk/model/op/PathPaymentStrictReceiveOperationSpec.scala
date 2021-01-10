package stellar.sdk.model.op

import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.json4s.{Formats, NoTypeHints}
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class PathPaymentStrictReceiveOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[PathPaymentStrictReceiveOperation]] = Arbitrary(genTransacted(genPathPaymentStrictReceiveOperation))
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "path payment operation" should {
    "serde via xdr" >> prop { actual: PathPaymentStrictReceiveOperation =>
      Operation.decode(actual.xdr) mustEqual actual
    }

    "parse from json" >> prop { op: Transacted[PathPaymentStrictReceiveOperation] =>
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
           |  "to":"${op.operation.destinationAccount.publicKey.accountId}",
           |  "path":[${if (op.operation.path.isEmpty) "" else op.operation.path.map(asset(_)).mkString("{", "},{", "}")}]
           |}
         """.stripMargin

      parse(doc).extract[Transacted[Operation]] mustEqual removeDestinationSubAccountId(op)
    }.setGen(genTransacted(genPathPaymentStrictReceiveOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

  // Because sub accounts are not yet supported in Horizon JSON.
  private def removeDestinationSubAccountId(op: Transacted[PathPaymentStrictReceiveOperation]): Transacted[PathPaymentStrictReceiveOperation] = {
    op.copy(operation = op.operation.copy(destinationAccount = op.operation.destinationAccount.copy(subAccountId = None)))
  }
}

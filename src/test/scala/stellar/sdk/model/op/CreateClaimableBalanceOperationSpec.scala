package stellar.sdk.model.op

import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.ClaimantGenerators
import stellar.sdk.util.ByteArrays

class CreateClaimableBalanceOperationSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val arbOp: Arbitrary[CreateClaimableBalanceOperation] = Arbitrary(genCreateClaimableBalanceOperation)
  implicit val arbTx: Arbitrary[Transacted[CreateClaimableBalanceOperation]] =
    Arbitrary(genTransacted(genCreateClaimableBalanceOperation))
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "create claimable balance operation" should {
    "serde via xdr bytes" >> prop { actual: CreateClaimableBalanceOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "serde via xdr string" >> prop { actual: CreateClaimableBalanceOperation =>
      Operation.decodeXDR(ByteArrays.base64(actual.encode)) mustEqual actual
    }

    "parse from json" >> prop { op: Transacted[CreateClaimableBalanceOperation] =>
      val doc =
        s"""
           |{
           |  "_links": {
           |    "self": {
           |      "href": "http://localhost:8000/operations/42949677057"
           |    },
           |    "transaction": {
           |      "href": "http://localhost:8000/transactions/25a461789570755483e3d3f078ae58ba5de2a083115c287e2ed14262107e0315"
           |    },
           |    "effects": {
           |      "href": "http://localhost:8000/operations/42949677057/effects"
           |    },
           |    "succeeds": {
           |      "href": "http://localhost:8000/effects?order=desc\u0026cursor=42949677057"
           |    },
           |    "precedes": {
           |      "href": "http://localhost:8000/effects?order=asc\u0026cursor=42949677057"
           |    }
           |  },
           |  "id": "${op.id}",
           |  "paging_token": "42949677057",
           |  "transaction_successful": true,
           |  "source_account": "${op.operation.sourceAccount.get.accountId}",
           |  "type": "create_claimable_balance",
           |  "type_i": 14,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "sponsor": "${op.operation.sourceAccount.get.accountId}",
           |  "asset": "${op.operation.amount.asset.stringEncode}",
           |  "amount": "${op.operation.amount.toDisplayUnits}",
           |  "claimants": [${op.operation.claimants.map(ClaimantGenerators.json).mkString(",")}]
           |}
         """.stripMargin

      parse(doc).extract[Transacted[CreateClaimableBalanceOperation]] mustEqual op
    }.setGen(genTransacted(genCreateClaimableBalanceOperation.suchThat(_.sourceAccount.nonEmpty)))
  }
}

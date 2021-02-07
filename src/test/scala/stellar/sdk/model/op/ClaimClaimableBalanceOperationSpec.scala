package stellar.sdk.model.op

import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.json4s.{Formats, NoTypeHints}
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.util.ByteArrays

class ClaimClaimableBalanceOperationSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val arbOp: Arbitrary[ClaimClaimableBalanceOperation] = Arbitrary(genClaimClaimableBalanceOperation)
  implicit val arbTx: Arbitrary[Transacted[ClaimClaimableBalanceOperation]] =
    Arbitrary(genTransacted(genClaimClaimableBalanceOperation))
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "create claimable balance operation" should {
    "serde via xdr bytes" >> prop { actual: ClaimClaimableBalanceOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "serde via xdr string" >> prop { actual: ClaimClaimableBalanceOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) mustEqual actual
    }

    "parse from json" >> prop { op: Transacted[ClaimClaimableBalanceOperation] =>
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
           |  "transaction_successful": true,
           |  "source_account": "${op.operation.sourceAccount.get.accountId}",
           |  "type": "claim_claimable_balance",
           |  "type_i": 15,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "balance_id": "${op.operation.id.encodeString}",
           |  "claimant": "${op.operation.sourceAccount.get.accountId}"
           |}
         """.stripMargin

      parse(doc).extract[Transacted[ClaimClaimableBalanceOperation]] mustEqual op
    }.setGen(genTransacted(genClaimClaimableBalanceOperation.suchThat(_.sourceAccount.nonEmpty)))
  }
}

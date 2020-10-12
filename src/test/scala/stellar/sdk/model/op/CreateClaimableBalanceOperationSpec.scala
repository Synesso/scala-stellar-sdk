package stellar.sdk.model.op

import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.util.ByteArrays

class CreateClaimableBalanceOperationSpec extends Specification with ArbitraryInput {

  implicit val arbOp: Arbitrary[CreateClaimableBalanceOperation] = Arbitrary(genCreateClaimableBalanceOperation)
  implicit val arbTx: Arbitrary[Transacted[CreateClaimableBalanceOperation]] =
    Arbitrary(genTransacted(genCreateClaimableBalanceOperation))

  "create claimable balance operation" should {
    "serde via xdr bytes" >> prop { actual: CreateClaimableBalanceOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "serde via xdr string" >> prop { actual: CreateClaimableBalanceOperation =>
      Operation.decodeXDR(ByteArrays.base64(actual.encode)) mustEqual actual
    }

/*
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
           |  "id": "42949677057",
           |  "paging_token": "42949677057",
           |  "transaction_successful": true,
           |  "source_account": "GCYTIVTAEF6AJOZG5TVXE7OZE7FLUXJUJSYAZ3IR2YH4MNINDJJX4DXF",
           |  "type": "create_claimable_balance",
           |  "type_i": 14,
           |  "created_at": "2020-10-11T03:00:16Z",
           |  "transaction_hash": "25a461789570755483e3d3f078ae58ba5de2a083115c287e2ed14262107e0315",
           |  "sponsor": "GCYTIVTAEF6AJOZG5TVXE7OZE7FLUXJUJSYAZ3IR2YH4MNINDJJX4DXF",
           |  "asset": "Dachshund:GCYTIVTAEF6AJOZG5TVXE7OZE7FLUXJUJSYAZ3IR2YH4MNINDJJX4DXF",
           |  "amount": "0.0005000",
           |  "claimants": [
           |    {
           |      "destination": "GAAYHQF2PNZ3H6LE5AX3UJSGUR2DQXHHGXYMHF32TDYF2FFPTTOFI3PA",
           |      "predicate": {
           |        "or": [
           |          {
           |            "and": [
           |              {
           |                "abs_before": "2020-10-11T03:00:12Z"
           |              },
           |              {
           |                "rel_before": "600"
           |              }
           |            ]
           |          },
           |          {
           |            "unconditional": true
           |          }
           |        ]
           |      }
           |    },
           |    {
           |      "destination": "GC6O6NSTRLQU3XSDB3SHF3T4K2WQ5DVXYYH7NPPN4K6M4CMHRCGPHCOF",
           |      "predicate": {
           |        "unconditional": true
           |      }
           |    }
           |  ]
           |}
         """.stripMargin
    }
*/
  }
}

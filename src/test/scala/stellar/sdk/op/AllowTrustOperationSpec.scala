package stellar.sdk.op

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.{AccountID, AllowTrustOp, AssetType}
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair}

class AllowTrustOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arb: Arbitrary[Transacted[AllowTrustOperation]] = Arbitrary(genTransacted(genAllowTrustOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "allow trust operation" should {
    "serde via xdr" >> prop { actual: AllowTrustOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: AllowTrustOperation => expected must beEquivalentTo(actual)
      }
    }

    "should not have native asset type" >> {
      val input = new AllowTrustOp
      input.setAsset(new AllowTrustOp.AllowTrustOpAsset)
      input.getAsset.setDiscriminant(AssetType.ASSET_TYPE_NATIVE)
      input.setTrustor(new AccountID)
      input.getTrustor.setAccountID(KeyPair.random.getXDRPublicKey)
      AllowTrustOperation.from(input) must beFailedTry[AllowTrustOperation]
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
           |  "source_account": "${op.sourceAccount.accountId}",
           |  "type": "allow_trust",
           |  "type_i": 7,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "asset_type": "${if (op.operation.assetCode.length <= 4) "credit_alphanum4" else "credit_alphanum12"}",
           |  "asset_code": "${op.operation.assetCode}",
           |  "asset_issuer": "${op.sourceAccount.accountId}"
           |  "trustor": "${op.operation.trustor.accountId}",
           |  "trustee": "${op.sourceAccount.accountId}",
           |  "authorize": ${op.operation.authorize}
           |}
         """.stripMargin

      parse(doc).extract[Transacted[AllowTrustOperation]] mustEqual op

    }
  }

}

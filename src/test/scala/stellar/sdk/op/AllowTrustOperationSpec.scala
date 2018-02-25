package stellar.sdk.op

import org.json4s.native.JsonMethods.parse
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.{AccountID, AllowTrustOp, AssetType}
import stellar.sdk._
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair}

class AllowTrustOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

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

/*
    "parse from json" >> prop { op: Transacted[AllowTrustOperation] =>


    /*
        "id": "32571670468239361",
        "paging_token": "32571670468239361",
        "source_account": "GDZC37RX2NTKXOG4TPSCZVI5PUSSNP2HJJJOIU7RWNAN5ZL2UHAF2HK3",
        "type": "allow_trust",
        "type_i": 7,
        "created_at": "2018-02-25T04:26:46Z",
        "transaction_hash": "2a47019ba6031d48ced24f3a6e0a6fb31ef97c6f1f8bafae0dfacda347465a84",
        "asset_type": "credit_alphanum12",
        "asset_code": "franjipani",
        "asset_issuer": "GDZC37RX2NTKXOG4TPSCZVI5PUSSNP2HJJJOIU7RWNAN5ZL2UHAF2HK3",
        "trustee": "GDZC37RX2NTKXOG4TPSCZVI5PUSSNP2HJJJOIU7RWNAN5ZL2UHAF2HK3",
        "trustor": "GAQUWIRXODT4OE3YE6L4NF3AYSR5ACEHPINM5S3J2F4XKH7FRZD4NDW2",
        "authorize": true

     */
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

      parse(doc).extract[Transacted[AllowTrustOperation]] mustEqual op

    }
*/
  }

}

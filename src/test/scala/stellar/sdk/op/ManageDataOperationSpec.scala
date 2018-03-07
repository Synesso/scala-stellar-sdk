package stellar.sdk.op

import org.apache.commons.codec.binary.Base64
import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, ByteArrays, DomainMatchers}

class ManageDataOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets with ByteArrays {

  implicit val arbDelete: Arbitrary[Transacted[DeleteDataOperation]] = Arbitrary(genTransacted(genDeleteDataOperation))
  implicit val arbWrite: Arbitrary[Transacted[WriteDataOperation]] = Arbitrary(genTransacted(genWriteDataOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  def doc[O <: ManageDataOperation](op: Transacted[O]) =
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
       |  "type": "manage_data",
       |  "type_i": 1,
       |  "created_at": "${formatter.format(op.createdAt)}",
       |  "transaction_hash": "${op.txnHash}",
       |  "name": "${op.operation.name}",
       |  "value": "${
      op.operation match {
        case WriteDataOperation(_, value, _) => Base64.encodeBase64String(value.getBytes("UTF-8"))
        case _ => ""
      }
    }"
       |}""".stripMargin

  "a write data operation" should {
    "serde via xdr" >> prop { actual: WriteDataOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: WriteDataOperation => expected must beEquivalentTo(actual)
      }
    }

    "parse from json" >> prop { op: Transacted[WriteDataOperation] =>
      parse(doc(op)).extract[Transacted[ManageDataOperation]] mustEqual op
    }
  }

  "a delete data operation" should {
    "serde via xdr" >> prop { actual: DeleteDataOperation =>
      Operation.fromXDR(actual.toXDR) must beSuccessfulTry.like {
        case expected: DeleteDataOperation => expected must beEquivalentTo(actual)
      }
    }

    "parse from json" >> prop { op: Transacted[DeleteDataOperation] =>
      parse(doc(op)).extract[Transacted[ManageDataOperation]] mustEqual op
    }
  }

}

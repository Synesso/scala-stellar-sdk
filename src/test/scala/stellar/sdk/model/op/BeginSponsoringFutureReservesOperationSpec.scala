package stellar.sdk.model.op

import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.util.ByteArrays

class BeginSponsoringFutureReservesOperationSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val arbOp: Arbitrary[BeginSponsoringFutureReservesOperation] =
    Arbitrary(genBeginSponsoringFutureReservesOperation)
  implicit val arbTx: Arbitrary[Transacted[BeginSponsoringFutureReservesOperation]] =
    Arbitrary(genTransacted(genBeginSponsoringFutureReservesOperation))
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "begin sponsoring future reserves operation" should {
    "serde via xdr bytes" >> prop { actual: BeginSponsoringFutureReservesOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "serde via xdr string" >> prop { actual: BeginSponsoringFutureReservesOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) mustEqual actual
    }

    "parse from json" >> prop { op: Transacted[BeginSponsoringFutureReservesOperation] =>
      val doc =
        s"""{
           |  "id": "${op.id}",
           |  "source_account": "${op.operation.sourceAccount.get.accountId}",
           |  "type": "begin_sponsoring_future_reserves",
           |  "type_i": 16,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "sponsored_id": "${op.operation.sponsored.publicKey.accountId}"
           |}
         """.stripMargin

      parse(doc).extract[Transacted[BeginSponsoringFutureReservesOperation]] mustEqual op
    }.setGen(genTransacted(genBeginSponsoringFutureReservesOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

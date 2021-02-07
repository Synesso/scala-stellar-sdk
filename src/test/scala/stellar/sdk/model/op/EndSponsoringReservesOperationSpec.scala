package stellar.sdk.model.op

import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.util.ByteArrays

class EndSponsoringReservesOperationSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val arbOp: Arbitrary[EndSponsoringFutureReservesOperation] =
    Arbitrary(genEndSponsoringFutureReservesOperation)
  implicit val arbTx: Arbitrary[Transacted[EndSponsoringFutureReservesOperation]] =
    Arbitrary(genTransacted(genEndSponsoringFutureReservesOperation))
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  "End sponsoring future reserves operation" should {
    "serde via xdr bytes" >> prop { actual: EndSponsoringFutureReservesOperation =>
      Operation.decodeXdr(actual.xdr) mustEqual actual
    }

    "serde via xdr string" >> prop { actual: EndSponsoringFutureReservesOperation =>
      Operation.decodeXdrString(actual.xdr.encode().base64()) mustEqual actual
    }

    "parse from json" >> prop { op: Transacted[EndSponsoringFutureReservesOperation] =>
      val doc =
        s"""{
           |  "id": "${op.id}",
           |  "source_account": "${op.operation.sourceAccount.get.accountId}",
           |  "type": "end_sponsoring_future_reserves",
           |  "type_i": 17,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "sponsored_id": "IGNORED"
           |}""".stripMargin

      parse(doc).extract[Transacted[EndSponsoringFutureReservesOperation]] mustEqual op
    }.setGen(genTransacted(genEndSponsoringFutureReservesOperation.suchThat(_.sourceAccount.nonEmpty)))
  }



}

package stellar.sdk.model.op

import org.apache.commons.codec.binary.Base64
import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.mutable.Specification
import stellar.sdk.util.ByteArrays.base64
import stellar.sdk.{ArbitraryInput, DomainMatchers, PublicKey}

class ManageDataOperationSpec extends Specification with ArbitraryInput with DomainMatchers with JsonSnippets {

  implicit val arbDelete: Arbitrary[Transacted[DeleteDataOperation]] = Arbitrary(genTransacted(genDeleteDataOperation))
  implicit val arbWrite: Arbitrary[Transacted[WriteDataOperation]] = Arbitrary(genTransacted(genWriteDataOperation))
  implicit val formats = Serialization.formats(NoTypeHints) + TransactedOperationDeserializer

  def doc[O <: ManageDataOperation](op: Transacted[O]) = {
    val dataValue = op.operation match {
      case WriteDataOperation(_, value, _) => Base64.encodeBase64String(value)
      case _ => ""
    }

    s"""
      |{
      |  "_links": {
      |    "self": {"href": "https://horizon-testnet.stellar.org/operations/10157597659144"},
      |    "transaction": {"href": "https://horizon-testnet.stellar.org/transactions/17a670bc424ff5ce3b386dbfaae9990b66a2a37b4fbe51547e8794962a3f9e6a"},
      |    "effects": {"href": "https://horizon-testnet.stellar.org/operations/10157597659144/effects"},
      |    "succeeds": {"href": "https://horizon-testnet.stellar.org/effects?order=desc\u0026cursor=10157597659144"},
      |    "precedes": {"href": "https://horizon-testnet.stellar.org/effects?order=asc\u0026cursor=10157597659144"}
      |  },
      |  "id": "${op.id}",
      |  "paging_token": "10157597659137",
      |  "source_account": "${op.operation.sourceAccount.get.accountId}",
      |  "type": "manage_data",
      |  "type_i": 1,
      |  "created_at": "${formatter.format(op.createdAt)}",
      |  "transaction_hash": "${op.txnHash}",
      |  "name": "${op.operation.name}",
      |  "value": "$dataValue"
      |}""".stripMargin
  }

  "a write data operation" should {
    "serde via xdr string" >> prop { actual: WriteDataOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: WriteDataOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded must beEquivalentTo(actual)
      remaining must beEmpty
    }

    "parse from json" >> prop { op: Transacted[WriteDataOperation] =>
      parse(doc(op)).extract[Transacted[ManageDataOperation]] must beEquivalentTo(op)
    }.setGen(genTransacted(genWriteDataOperation.suchThat(_.sourceAccount.nonEmpty)))

    "encode a string payload as UTF-8 in base64" >> prop { (s: String, source: PublicKey) =>
      val value = new String(s.take(64).getBytes("UTF-8").take(60), "UTF-8")
      WriteDataOperation("name", value).value.toSeq mustEqual value.getBytes("UTF-8").toSeq
      WriteDataOperation("name", value, None).value.toSeq mustEqual value.getBytes("UTF-8").toSeq
      WriteDataOperation("name", value, Some(source)).value.toSeq mustEqual value.getBytes("UTF-8").toSeq
    }.setGen1(Arbitrary.arbString.arbitrary.suchThat(_.nonEmpty))

    "fail if the key is greater than 64 bytes" >> prop { s: String =>
      WriteDataOperation(s, "value") must throwAn[IllegalArgumentException]
    }.setGen(Gen.identifier.suchThat(_.getBytes("UTF-8").length > 64))

    "fail if the value is greater than 64 bytes" >> prop { s: String =>
      WriteDataOperation("name", s) must throwAn[IllegalArgumentException]
    }.setGen(Gen.identifier.suchThat(_.getBytes("UTF-8").length > 64))
  }

  "a delete data operation" should {
    "serde via xdr string" >> prop { actual: DeleteDataOperation =>
      Operation.decodeXDR(base64(actual.encode)) must beEquivalentTo(actual)
    }

    "serde via xdr bytes" >> prop { actual: DeleteDataOperation =>
      val (remaining, decoded) = Operation.decode.run(actual.encode).value
      decoded mustEqual actual
      remaining must beEmpty
    }

    "parse from json" >> prop { op: Transacted[DeleteDataOperation] =>
      parse(doc(op)).extract[Transacted[ManageDataOperation]] mustEqual op
    }.setGen(genTransacted(genDeleteDataOperation.suchThat(_.sourceAccount.nonEmpty)))
  }

}

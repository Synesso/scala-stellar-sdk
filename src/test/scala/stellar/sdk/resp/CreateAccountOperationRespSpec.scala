package stellar.sdk.resp

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import org.json4s.native.JsonMethods._
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk.{Amount, ArbitraryInput}

class CreateAccountOperationRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + OperationRespDeserializer
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

  def amountString(a: Amount): String = f"${a.units / math.pow(10, 7)}%.7f"

  "a create account operation document" should {
    "parse to a create account operation " >> prop { op: OperationCreateAccount =>
      val doc =
        s"""
           |{
           |  "_links": {
           |    "self": {"href": "https://horizon-testnet.stellar.org/operations/10157597659137"},
           |    "transaction": {"href": "https://horizon-testnet.stellar.org/transactions/17a670bc424ff5ce3b386dbfaae9990b66a2a37b4fbe51547e8794962a3f9e6a"},
           |    "effects": {"href": "https://horizon-testnet.stellar.org/operations/10157597659137/effects"},
           |    "succeeds": {"href": "https://horizon-testnet.stellar.org/effects?order=desc\u0026cursor=10157597659137"},
           |    "precedes": {"href": "https://horizon-testnet.stellar.org/effects?order=asc\u0026cursor=10157597659137"}
           |  },
           |  "id": "${op.id}",
           |  "paging_token": "10157597659137",
           |  "source_account": "${op.funder.accountId}",
           |  "type": "create_account",
           |  "type_i": 0,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  "starting_balance": "${amountString(op.startingBalance)}",
           |  "funder": "${op.funder.accountId}",
           |  "account": "${op.account.accountId}"
           |}
         """.stripMargin

      parse(doc).extract[OperationResp] mustEqual op
    }
  }
}

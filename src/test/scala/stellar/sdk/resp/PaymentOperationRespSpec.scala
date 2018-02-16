package stellar.sdk.resp

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk.{Amount, ArbitraryInput, NonNativeAsset}

class PaymentOperationRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + OperationRespDeserializer
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

  def amountString(a: Amount): String = f"${a.units / math.pow(10, 7)}%.7f"

  def amountDocPortion(amount: Amount): String = {
    amount.asset match {
      case nn: NonNativeAsset =>
        s""""amount": "${amountString(amount)}",
           |"asset_type": "${nn.typeString}",
           |"asset_code": "${nn.code}",
           |"asset_issuer": "${nn.issuer.accountId}"
        """.stripMargin.trim

      case _ =>
        s""""amount": "${amountString(amount)}",
           |"asset_type": "native"
        """.stripMargin.trim
    }
  }

  "a create account operation document" should {
    "parse to a create account operation " >> prop { op: OperationPayment =>
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
           |  ${amountDocPortion(op.amount)},
           |  "from": "${op.fromAccount.accountId}",
           |  "to": "${op.toAccount.accountId}",
           |}
         """.stripMargin

      parse(doc).extract[OperationResp] mustEqual op
    }
  }
}

package stellar.sdk.resp

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk.{Amount, ArbitraryInput, Asset, NonNativeAsset}

class PathPaymentOperationRespSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + OperationRespDeserializer
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

  def amountString(a: Amount): String = f"${a.units / math.pow(10, 7)}%.7f"

  def amountDocPortion(amount: Amount, label: String = "amount", assetPrefix: String = ""): String = {
    s"""
      |"$label":${amountString(amount)}
      |${asset(amount.asset, assetPrefix)}
    """.stripMargin
  }

  def asset(a: Asset, assetPrefix: String = ""): String = a match {
    case nn: NonNativeAsset =>
      s"""
         |"${assetPrefix}asset_type": "${nn.typeString}",
         |"${assetPrefix}asset_code": "${nn.code}",
         |"${assetPrefix}asset_issuer": "${nn.issuer.accountId}"
        """.stripMargin.trim

    case _ =>
      s"""
         |"${assetPrefix}asset_type": "native"
        """.stripMargin.trim
  }

  "a create account operation document" should {
    "parse to a create account operation " >> prop { op: OperationPathPayment =>
      val doc =
        s"""
           |{
           |  "_links":{
           |    "self":{"href":"https://horizon-testnet.stellar.org/operations/940258535411713"},
           |    "transaction":{"href":"https://horizon-testnet.stellar.org/transactions/a995af17837d1b53fb5782269250a36e9dbe74170260b46f2708e5f23f7c864a"},
           |    "effects":{"href":"https://horizon-testnet.stellar.org/operations/940258535411713/effects"},
           |    "succeeds":{"href":"https://horizon-testnet.stellar.org/effects?order=desc&cursor=940258535411713"},
           |    "precedes":{"href":"https://horizon-testnet.stellar.org/effects?order=asc&cursor=940258535411713"}
           |  },
           |  "id": "${op.id}",
           |  "paging_token": "10157597659137",
           |  "source_account": "${op.sourceAccount.accountId}",
           |  "type":"path_payment",
           |  "type_i":2,
           |  "created_at": "${formatter.format(op.createdAt)}",
           |  "transaction_hash": "${op.txnHash}",
           |  ${amountDocPortion(op.toAmount)}
           |  ${amountDocPortion(op.fromMaxAmount, "source_max", "source_")}
           |  "from":"${op.fromAccount.accountId}",
           |  "to":"${op.toAccount.accountId}",
           |  "path":[${if (op.path.isEmpty) "" else op.path.map(asset(_)).mkString("{","},{", "}")}]
           |}
         """.stripMargin

      parse(doc).extract[OperationResp] mustEqual op
    }
  }
}

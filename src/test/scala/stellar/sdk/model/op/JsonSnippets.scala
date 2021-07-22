package stellar.sdk.model.op

import stellar.sdk.model.{AccountId, Amount, Asset, NonNativeAsset}

import java.time.format.DateTimeFormatter
import java.util.Locale

trait JsonSnippets {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  def amountString(a: Amount): String = "%.7f".formatLocal(Locale.ROOT, a.units / math.pow(10, 7))

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

  def opt(key: String, value: Option[Any]) = value.map {
    case v: String => s""""$key":"$v","""
    case v: Set[_] => s""""$key":[${
      v.map {
        case s: String => s""""$s""""
        case a => a
      }.mkString(",")
    }],"""
    case v => s""""$key":$v,"""
  }.getOrElse("")

  def accountId(accountId: AccountId, prefix: String): String =
    s"""
       |"$prefix": "${accountId.publicKey.accountId}",
       |${accountId.subAccountId.map(id => s""""${prefix}_muxed_id": "${id}",""").getOrElse("")}
       |""".stripMargin.trim

}

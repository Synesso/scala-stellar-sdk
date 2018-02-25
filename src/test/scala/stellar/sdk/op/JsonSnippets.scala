package stellar.sdk.op

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import stellar.sdk.{Amount, Asset, NonNativeAsset}

trait JsonSnippets {
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

  def opt(key: String, value: Option[Any]) = value.map{
    case v: String => s""""$key":"$v","""
    case v: Set[_] => s""""$key":[${v.map {
      case s: String => s""""$s""""
      case a => a
    }.mkString(",")}],"""
    case v => s""""$key":$v,"""
  }.getOrElse("")

}

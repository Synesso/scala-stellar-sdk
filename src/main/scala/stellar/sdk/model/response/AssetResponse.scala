package stellar.sdk.model.response

import org.json4s.{DefaultFormats, Formats}
import org.json4s.JsonAST.JObject
import stellar.sdk._
import stellar.sdk.model.{Amount, IssuedAsset12, IssuedAsset4, NonNativeAsset}

case class AssetResponse(
  asset: NonNativeAsset,
  balances: AssetBalances,
  numAccounts: Int,
  authRequired: Boolean,
  authRevocable: Boolean
) {
  def amount: Long = balances.total
}

case class AssetBalances(
  authorized: Long,
  authorizedToMaintainLiabilities: Long,
  unauthorized: Long
) {
  def total: Long = authorized + authorizedToMaintainLiabilities + unauthorized
}

object AssetRespDeserializer extends ResponseParser[AssetResponse]({ o: JObject =>
  implicit val formats: Formats = DefaultFormats + AssetBalancesDeserializer
  val asset = {
    val code = (o \ "asset_code").extract[String]
    val issuer = KeyPair.fromAccountId((o \ "asset_issuer").extract[String])
    (o \ "asset_type").extract[String] match {
      case "credit_alphanum4" => IssuedAsset4(code, issuer)
      case "credit_alphanum12" => IssuedAsset12(code, issuer)
      case t => throw new RuntimeException(s"Unrecognised asset type: $t")
    }
  }
  val balances = (o \ "balances").extract[AssetBalances]

  val amount = Amount.toBaseUnits((o \ "amount").extract[String].toDouble).getOrElse(
    throw new RuntimeException(s"Invalid asset amount: ${(o \ "amount").extract[Double]}"))
  val numAccounts = (o \ "num_accounts").extract[Int]
  val authRequired = (o \ "flags" \ "auth_required").extract[Boolean]
  val authRevocable = (o \ "flags" \ "auth_revocable").extract[Boolean]
  AssetResponse(asset, balances, numAccounts, authRequired, authRevocable)
})

object AssetBalancesDeserializer extends ResponseParser[AssetBalances]({ o: JObject =>
  implicit val formats: Formats = DefaultFormats
  def extractToLong(key: String) = Amount.toBaseUnits((o \ key).extract[String].toDouble).get
  AssetBalances(
    authorized = extractToLong("authorized"),
    authorizedToMaintainLiabilities = extractToLong("authorized_to_maintain_liabilities"),
    unauthorized = extractToLong("unauthorized")
  )
})
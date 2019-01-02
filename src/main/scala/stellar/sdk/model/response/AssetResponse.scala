package stellar.sdk.model.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk._
import stellar.sdk.model.{Amount, IssuedAsset12, IssuedAsset4, NonNativeAsset}

case class AssetResponse(asset: NonNativeAsset, amount: Long, numAccounts: Int, authRequired: Boolean, authRevocable: Boolean)

object AssetRespDeserializer extends ResponseParser[AssetResponse]({ o: JObject =>
  implicit val formats = DefaultFormats
  val asset = {
    val code = (o \ "asset_code").extract[String]
    val issuer = KeyPair.fromAccountId((o \ "asset_issuer").extract[String])
    (o \ "asset_type").extract[String] match {
      case "credit_alphanum4" => IssuedAsset4(code, issuer)
      case "credit_alphanum12" => IssuedAsset12(code, issuer)
      case t => throw new RuntimeException(s"Unrecognised asset type: $t")
    }
  }
  val amount = Amount.toBaseUnits((o \ "amount").extract[String].toDouble).getOrElse(
    throw new RuntimeException(s"Invalid asset amount: ${(o \ "amount").extract[Double]}"))
  val numAccounts = (o \ "num_accounts").extract[Int]
  val authRequired = (o \ "flags" \ "auth_required").extract[Boolean]
  val authRevocable = (o \ "flags" \ "auth_revocable").extract[Boolean]
  AssetResponse(asset, amount, numAccounts, authRequired, authRevocable)
})

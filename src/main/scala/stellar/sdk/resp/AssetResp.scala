package stellar.sdk.resp

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk._

case class AssetResp(asset: Asset, amount: Long, numAccounts: Int, authRequired: Boolean, authRevocable: Boolean)

class AssetRespDeserializer extends CustomSerializer[AssetResp](format => ({
  case o: JObject =>
    implicit val formats = DefaultFormats
    val asset = {
      val code = (o \ "asset_code").extract[String]
      val issuer = KeyPair.fromAccountId((o \ "asset_issuer").extract[String])
      (o \ "asset_type").extract[String] match {
        case "credit_alphanum4" => AssetTypeCreditAlphaNum4(code, issuer)
        case "credit_alphanum12" => AssetTypeCreditAlphaNum12(code, issuer)
        case t => throw new RuntimeException(s"Unrecognised asset type: $t")
      }
    }
    val amount = Amount.toBaseUnits((o \ "amount").extract[String].toDouble).getOrElse(
      throw new RuntimeException(s"Invalid asset amount: ${(o \ "amount").extract[Double]}"))
    val numAccounts = (o \ "num_accounts").extract[Int]
    val authRequired = (o \ "flags" \ "auth_required").extract[Boolean]
    val authRevocable = (o \ "flags" \ "auth_revocable").extract[Boolean]
    AssetResp(asset, amount, numAccounts, authRequired, authRevocable)
}, PartialFunction.empty)
)

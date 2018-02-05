package stellar.sdk.resp

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk._

sealed trait EffectResp {
  val id: String
}

case class EffectAccountCreated(id: String, account: PublicKeyOps, startingBalance: NativeAmount) extends EffectResp


class EffectRespDeserializer extends CustomSerializer[EffectResp](format => ({
  case o: JObject =>
    implicit val formats = DefaultFormats
    val id = (o \ "id").extract[String]
    (o \ "type").extract[String] match {
      case "account_created" =>
        val account = KeyPair.fromAccountId((o \ "account").extract[String])
        val startingBalance = Amount.lumens((o \ "starting_balance").extract[String].toDouble).get
        EffectAccountCreated(id, account, startingBalance)
      case t => throw new RuntimeException(s"Unrecognised effect type '$t'")
    }
}, PartialFunction.empty)
)

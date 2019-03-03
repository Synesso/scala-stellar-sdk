package stellar.sdk.model.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods
import stellar.sdk.{KeyPair, PublicKey}
import stellar.sdk.model.Memo

case class FederationResponse(address: String,
                              account: PublicKey,
                              memo: Option[Memo] = None)

object FederationResponseDeserialiser extends ResponseParser[FederationResponse]({ o: JObject =>
  implicit val formats = DefaultFormats

  println(JsonMethods.pretty(JsonMethods.render(o)))

  FederationResponse(
    address = (o \ "stellar_address").extract[String],
    account = KeyPair.fromAccountId((o \ "account_id").extract[String])
  )
})
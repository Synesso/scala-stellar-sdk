package stellar.sdk.model.response

import okio.ByteString
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.model._
import stellar.sdk.util.ByteArrays.{hexToBytes, trimmedByteArray}
import stellar.sdk.{KeyPair, PublicKey}

case class FederationResponse(address: String,
                              account: PublicKey,
                              memo: Memo = NoMemo)

object FederationResponseDeserialiser extends ResponseParser[FederationResponse]({ o: JObject =>
  implicit val formats = DefaultFormats

//  println(JsonMethods.pretty(JsonMethods.render(o)))

  FederationResponse(
    // reference server erroneously fails to set `stellar_address` for forward lookups
    address = (o \ "stellar_address").extractOpt[String].orNull,
    // reference server erroneously fails to set `account_id` for reverse lookups
    account = (o \ "account_id").extractOpt[String].map(KeyPair.fromAccountId).orNull,
    memo = (o \ "memo_type").extractOpt[String] match {
      case Some("id") => MemoId((o \ "memo").extract[String].toLong)
      case Some("text") => MemoText((o \ "memo").extract[String])
      case Some("hash") => MemoHash(ByteString.decodeHex((o \ "memo").extract[String]))
      case _ => NoMemo
    }
  )
})
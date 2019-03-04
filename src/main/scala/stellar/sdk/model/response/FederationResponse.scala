package stellar.sdk.model.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods
import stellar.sdk.{KeyPair, PublicKey}
import stellar.sdk.model._
import stellar.sdk.util.ByteArrays
import stellar.sdk.util.ByteArrays.{hexToBytes, trimmedByteArray}

case class FederationResponse(address: String,
                              account: PublicKey,
                              memo: Memo = NoMemo)

object FederationResponseDeserialiser extends ResponseParser[FederationResponse]({ o: JObject =>
  implicit val formats = DefaultFormats

//  println(JsonMethods.pretty(JsonMethods.render(o)))

  FederationResponse(
    // reference server erroneously fails to set `stellar_address` for forward lookups
    address = (o \ "stellar_address").extractOpt[String].orNull,
    account = KeyPair.fromAccountId((o \ "account_id").extract[String]),
    memo = (o \ "memo_type").extractOpt[String] match {
      case Some("id") => MemoId((o \ "memo").extract[String].toLong)
      case Some("text") => MemoText((o \ "memo").extract[String])
      case Some("hash") => MemoHash(trimmedByteArray(hexToBytes((o \ "memo").extract[String])))
      case _ => NoMemo
    }
  )
})
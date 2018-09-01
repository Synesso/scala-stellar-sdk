package stellar.sdk.resp

import akka.http.scaladsl.model.Uri
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.inet.TxnFailure

object TxnFailureDeserializer extends ResponseParser[TxnFailure]({ o: JObject =>
  implicit val formats = DefaultFormats

  TxnFailure(
    uri = Uri("file://unknown"), // overwritten
    status = (o \ "status").extract[Int],
    detail = (o \ "detail").extract[String],
    resultCode = (o \ "extras" \ "result_codes" \ "transaction").extractOpt[String],
    operationResultCodes = (o \ "extras" \ "result_codes" \ "operations").extractOpt[Array[String]],
    resultXDR = (o \ "extras" \ "result_xdr").extractOpt[String]
  )
})

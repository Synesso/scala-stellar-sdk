package stellar.sdk.resp

import com.softwaremill.sttp._
import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk.inet.TxnFailure

object TxnFailureDeserializer extends CustomSerializer[TxnFailure](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats

    //    import org.json4s.native.JsonMethods._
    //    println(pretty(render(o)))

    TxnFailure(
      uri = uri"file://unknown", // overwritten
      status = (o \ "status").extract[Int],
      detail = (o \ "detail").extract[String],
      resultCode = (o \ "extras" \ "result_codes" \ "transaction").extractOpt[String],
      operationResultCodes = (o \ "extras" \ "result_codes" \ "operations").extractOpt[Array[String]],
      resultXDR = (o \ "extras" \ "result_xdr").extractOpt[String]
    )
}, PartialFunction.empty))

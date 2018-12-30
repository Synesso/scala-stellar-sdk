package stellar.sdk.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject

case class DataValueResp(v: String)

object DataValueRespDeserializer extends ResponseParser[DataValueResp]({ o: JObject =>
    implicit val formats = DefaultFormats
    DataValueResp((o \ "value").extract[String])
})

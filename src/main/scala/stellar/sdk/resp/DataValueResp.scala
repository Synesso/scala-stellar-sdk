package stellar.sdk.resp

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}

case class DataValueResp(v: String)

class DataValueRespDeserializer extends CustomSerializer[DataValueResp](format => ({
  case o: JObject =>
    implicit val formats = DefaultFormats
    DataValueResp((o \ "value").extract[String])
}, PartialFunction.empty)
)

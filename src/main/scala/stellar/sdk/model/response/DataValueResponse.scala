package stellar.sdk.model.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject

case class DataValueResponse(v: String)

object DataValueRespDeserializer extends ResponseParser[DataValueResponse]({ o: JObject =>
    implicit val formats = DefaultFormats
    DataValueResponse((o \ "value").extract[String])
})

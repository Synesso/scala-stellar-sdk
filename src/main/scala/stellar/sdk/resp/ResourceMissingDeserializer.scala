package stellar.sdk.resp

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk.inet.ResourceMissingException

class ResourceMissingDeserializer extends CustomSerializer[ResourceMissingException](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats
    val status = (o \ "status").extract[Int]
    val detail = (o \ "detail").extract[String]
    val instance = (o \ "instance").extract[String]
    ResourceMissingException(status, detail, instance)
}, PartialFunction.empty))

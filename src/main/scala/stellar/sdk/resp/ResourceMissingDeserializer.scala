package stellar.sdk.resp

import com.softwaremill.sttp._
import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk.inet.ResourceMissingException

object ResourceMissingDeserializer extends CustomSerializer[ResourceMissingException](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats
    val status = (o \ "status").extract[Int]
    val detail = (o \ "detail").extract[String]
    val instance = (o \ "instance").extract[String]
    ResourceMissingException(uri"file://unknown", status, detail, instance)
}, PartialFunction.empty))

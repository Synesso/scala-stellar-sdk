package stellar.sdk.inet

import org.json4s.JsonAST.JArray
import org.json4s.native.JsonParser._
import org.json4s.native.Serialization
import org.json4s.{CustomSerializer, NoTypeHints}

/**
  * A page of results
  */
case class Page[T](xs: Seq[T], nextLink: String)

/*
class PageDeserializer[T]()(implicit m: Manifest[T]) extends CustomSerializer[Page[T]](format => ({
  case o: JObject =>
    implicit val formats = Serialization.formats(NoTypeHints) + new AssetRespDeserializer + new EffectRespDeserializer
    val nextLink = (o \ "_links" \ "next" \ "href").extract[String]
    val JArray(recordsJson) = o \ "_embedded" \ "records"
    val records = recordsJson.map(_.extract[T])
    Page(records, nextLink)
}, PartialFunction.empty)
)
*/

object Page {
  def apply[T](js: String, de: CustomSerializer[T])(implicit m: Manifest[T]): Page[T] = {
    implicit val formats = Serialization.formats(NoTypeHints) + de
    val o = parse(js)
    val nextLink = (o \ "_links" \ "next" \ "href").extract[String]
    val JArray(recordsJson) = o \ "_embedded" \ "records"
    val records = recordsJson.map(_.extract[T])
    Page(records, nextLink)
  }
}

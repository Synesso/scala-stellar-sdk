package stellar.sdk.inet

import org.json4s.JsonAST.{JArray, JObject}
import org.json4s.native.Serialization
import org.json4s.{CustomSerializer, NoTypeHints}
import stellar.sdk.resp.{AssetRespDeserializer, EffectRespDeserializer}
/**
  * A page of results
  */
case class Page[T](xs: Seq[T], nextLink: String)

class PageDeserializer[T]()(implicit m: Manifest[T]) extends CustomSerializer[Page[T]](format => ({
  case o: JObject =>
    implicit val formats = Serialization.formats(NoTypeHints) + new AssetRespDeserializer + new EffectRespDeserializer
    val nextLink = (o \ "_links" \ "next" \ "href").extract[String]
    val JArray(recordsJson) = o \ "_embedded" \ "records"
    val records = recordsJson.map(_.extract[T])
    Page(records, nextLink)
}, PartialFunction.empty)
)

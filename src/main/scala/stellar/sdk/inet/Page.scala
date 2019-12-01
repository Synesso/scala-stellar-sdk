package stellar.sdk.inet

import okhttp3.HttpUrl
import org.json4s.JsonAST.JArray
import org.json4s.{DefaultFormats, Formats, JObject, JValue}
import stellar.sdk.model.response.ResponseParser

/**
  * A page of results
  */
case class Page[T](xs: List[T], nextLink: Option[HttpUrl])

case class RawPage(inner: List[JValue], nextLink: Option[String]) {
  def parse[T](sourceLink: HttpUrl)(implicit formats: Formats, m: Manifest[T]): Page[T] =
    Page(inner.map(_.extract[T]), nextLink.map(HttpUrl.parse).filter(_ != sourceLink))
}

object RawPageDeserializer extends ResponseParser[RawPage]({ o: JObject =>
  implicit val formats = DefaultFormats

  val nextLink = (o \ "_links" \ "next" \ "href").extractOpt[String]
  val JArray(records) = o \ "_embedded" \ "records"

  RawPage(records, nextLink)
})

package stellar.sdk.inet

import org.json4s.JsonAST.JArray
import org.json4s.{DefaultFormats, Formats, JObject, JValue}
import stellar.sdk.model.response.ResponseParser

/**
  * A page of results
  */
case class Page[T](xs: Seq[T], nextLink: Option[String])

case class RawPage(inner: Seq[JValue], nextLink: Option[String]) {
  def parse[T](implicit formats: Formats, m: Manifest[T]): Page[T] = Page(inner.map(_.extract[T]), nextLink)
}

object RawPageDeserializer extends ResponseParser[RawPage]({ o: JObject =>
  implicit val formats = DefaultFormats

  val nextLink = (o \ "_links" \ "next" \ "href").extractOpt[String]
  val JArray(records) = o \ "_embedded" \ "records"

  RawPage(records, nextLink)
})

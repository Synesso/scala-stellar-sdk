package stellar.sdk.inet

import org.json4s.JsonAST.{JArray, JString}
import org.json4s.native.JsonMethods._
import org.json4s.{CustomSerializer, DefaultFormats}

/**
  * A page of results
  */
case class Page[T](xs: Seq[T], nextLink: String)

object Page {
  def apply[T](js: String, de: CustomSerializer[T])(implicit m: Manifest[T]): Page[T] = {
    implicit val formats = DefaultFormats + de
    val o = parse(js)
    val JString(nextLink) = o \ "_links" \ "next" \ "href"
    val JArray(recordsJson) = o \ "_embedded" \ "records"
    val records = recordsJson.map(_.extract[T])
    Page(records, nextLink)
  }
}

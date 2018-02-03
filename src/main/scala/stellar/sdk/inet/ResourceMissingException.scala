package stellar.sdk.inet

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import stellar.sdk.resp.ResourceMissingDeserializer

import scala.util.Try

case class ResourceMissingException(status: Int, detail: String, instance: String) extends Exception(detail)

object ResourceMissingException {
  implicit val formats = Serialization.formats(NoTypeHints) + new ResourceMissingDeserializer

  def apply(s: String): Try[ResourceMissingException] = Try {
    parse(s).extract[ResourceMissingException]
  }
}

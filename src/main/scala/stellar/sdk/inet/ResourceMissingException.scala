package stellar.sdk.inet

import com.softwaremill.sttp.Uri
import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import stellar.sdk.resp.ResourceMissingDeserializer

import scala.util.Try

case class ResourceMissingException(uri: Uri, status: Int, detail: String, instance: String)
  extends Exception(s"Uri: $uri - $detail")

object ResourceMissingException {
  implicit val formats = Serialization.formats(NoTypeHints) + new ResourceMissingDeserializer

  def apply(uri: Uri, s: String): Try[ResourceMissingException] = Try {
    parse(s).extract[ResourceMissingException].copy(uri = uri)
  }
}

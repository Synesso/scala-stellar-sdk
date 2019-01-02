package stellar.sdk.model.response

import org.json4s.native.JsonMethods.{pretty, render}
import org.json4s.{CustomSerializer, JObject}

import scala.util.control.NonFatal

class ResponseParser[T](f: JObject => T)(implicit m: Manifest[T]) extends CustomSerializer[T](_ => ({
  case o: JObject =>
    try {
      f(o)
    } catch {
      case NonFatal(t) => throw ResponseParseException(pretty(render(o)), t)
    }
}, PartialFunction.empty))

case class ResponseParseException(doc: String, cause: Throwable)
  extends Exception(s"Unable to parse document:\n$doc", cause)

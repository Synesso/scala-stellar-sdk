package stellar.scala.sdk.inet

import java.net.URI

import scala.util.Try

case class Server(uri: URI) {

}

object Server {
  def apply(uri: String): Try[Server] = Try {
    Server(URI.create(uri))
  }
}

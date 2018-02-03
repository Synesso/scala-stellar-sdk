package stellar.sdk.inet

import java.net.URI

import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp.json4s._
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import stellar.sdk.SignedTransaction
import stellar.sdk.resp.{AccountRespDeserializer, SubmitTransactionResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

case class Server(uri: URI) {
  implicit val backend = AkkaHttpBackend()
  implicit val formats = Serialization.formats(NoTypeHints) + new AccountRespDeserializer

  def post(txn: SignedTransaction): Future[SubmitTransactionResponse] = {
    ???
  }

  def get[T: ClassTag](path: String, params: Map[String, String] = Map.empty)(implicit ec: ExecutionContext, m: Manifest[T]): Future[T] = {
    for {
      resp <- sttp.get(uri"$uri/$path?$params").response(asJson[T]).send()
    } yield resp.body match {
      case Right(r) => r
      case Left(s) => throw ResourceMissingException(s).getOrElse(new RuntimeException(s"Unrecognised response: $s"))
    }
  }

  def getPages[T: ClassTag](path: String)(implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]] = {
    val page: Future[Page[T]] = getPage(path)
    page.map(p => p.xs.toStream ++ Stream.empty[T]) // todo - stream.empty to be next page
  }

  private def getPage[T: ClassTag](path: String)(implicit ec: ExecutionContext, m: Manifest[T]): Future[Page[T]] = {
    ???
  }

}

object Server {
  def apply(uri: String): Try[Server] = Try {
    Server(URI.create(uri))
  }
}

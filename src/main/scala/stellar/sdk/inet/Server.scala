package stellar.sdk.inet

import java.net.URI

import stellar.sdk.resp.{AccountRespDeserializer, SubmitTransactionResponse}
import stellar.sdk.{PublicKeyOps, SignedTransaction}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp.json4s._
import org.json4s.NoTypeHints
import org.json4s.native.Serialization

import scala.reflect.ClassTag

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
      case Left(s) => throw new RuntimeException(s"Unrecognised reponse: $s")
    }
  }
}

object Server {
  def apply(uri: String): Try[Server] = Try {
    Server(URI.create(uri))
  }
}

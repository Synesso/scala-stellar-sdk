package stellar.sdk.inet

import java.net.URI

import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp.json4s._
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import stellar.sdk.SignedTransaction
import stellar.sdk.resp.{AccountRespDeserializer, AssetResp, AssetRespDeserializer, SubmitTransactionResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try


case class Server(uri: URI) {
  implicit val backend = AkkaHttpBackend()
  implicit val formats = Serialization.formats(NoTypeHints) + new AccountRespDeserializer +
    new AssetRespDeserializer + new PageDeserializer[AssetResp]()

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

  def getStream[T: ClassTag](path: String)(implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]] = {
    def next(p: Page[T]): Future[Option[Page[T]]] =
      (getPageAbsoluteUri(uri"${p.nextLink}"): Future[Page[T]]).map(Some(_)).recover { case e: ResourceMissingException => None }

    def stream(ts: Seq[T], maybeNextPage: Future[Option[Page[T]]]): Stream[T] = {
      ts match {
        case Nil =>
          Await.result(maybeNextPage, 30.seconds) match {
            case None =>
              Stream.empty[T]
            case Some(nextPage) =>
              val maybeNextPage = next(nextPage)
              stream(nextPage.xs, maybeNextPage)
          }
        case h +: t =>
          Stream.cons(h, stream(t, maybeNextPage))
      }
    }
    (getPage(path): Future[Page[T]]).map { p0: Page[T] => stream(p0.xs, next(p0)) }
  }

  def getPage[T: ClassTag](path: String)(implicit ec: ExecutionContext, m: Manifest[T]): Future[Page[T]] =
    getPageAbsoluteUri(uri"$uri/$path")

  private def getPageAbsoluteUri[T: ClassTag](uri: Uri)(implicit ec: ExecutionContext, m: Manifest[T]): Future[Page[T]] = {
    for {
      resp <- sttp.get(uri).response(asJson[Page[T]]).send()
    } yield resp.body match {
      case Right(r) => r
      case Left(s) => throw ResourceMissingException(s).getOrElse(new RuntimeException(s"Unrecognised response: $s"))
    }
  }
}

object Server {
  def apply(uri: String): Try[Server] = Try {
    Server(URI.create(uri))
  }
}

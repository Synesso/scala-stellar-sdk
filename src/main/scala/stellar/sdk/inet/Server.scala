package stellar.sdk.inet

import java.net.URI

import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp.json4s._
import org.json4s.native.Serialization
import org.json4s.{CustomSerializer, NoTypeHints}
import stellar.sdk.op.TransactedOperationDeserializer
import stellar.sdk.resp._
import stellar.sdk.{OrderBookDeserializer, SignedTransaction}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try


case class Server(uri: URI) {
  implicit val backend = AkkaHttpBackend()
  implicit val formats = Serialization.formats(NoTypeHints) + AccountRespDeserializer + DataValueRespDeserializer +
    LedgerRespDeserializer + TransactedOperationDeserializer + OrderBookDeserializer + TransactionPostRespDeserializer

  def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResp] = for {
    envelope <- Future.fromTry(txn.toEnvelopeXDRBase64)
    requestUri = uri"$uri/transactions"
    response <- sttp.body(Map("tx" -> envelope)).post(requestUri).response(asJson[TransactionPostResp]).send()
  } yield {
    response.body match {
      case Right(r) => r
      case Left(s) => throw TxnFailure(requestUri, s).getOrElse(new RuntimeException(s"Unrecognised response: $s"))
    }
  }

  def get[T: ClassTag](path: String, params: Map[String, Any] = Map.empty)(implicit ec: ExecutionContext, m: Manifest[T]): Future[T] = {
    val requestUri = uri"$uri/$path?$params"
    for {
      resp <- sttp.get(requestUri).response(asJson[T]).send()
    } yield {
      resp.body match {
        case Right(r) => r
        case Left(s) => throw TxnFailure(requestUri, s).getOrElse(new RuntimeException(s"Unrecognised response: $s"))
      }
    }
  }

  def getStream[T: ClassTag](path: String, de: CustomSerializer[T], params: Map[String, String] = Map.empty)
                            (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]] = {

    implicit val inner = de

    def next(p: Page[T]): Future[Option[Page[T]]] =
      (getPageAbsoluteUri(uri"${p.nextLink}"): Future[Page[T]]).map(Some(_)).recover { case e: TxnFailure => None }

    def stream(ts: Seq[T], maybeNextPage: Future[Option[Page[T]]]): Stream[T] = {
      ts match {
        case Nil =>
          Await.result(maybeNextPage, 30.seconds) match {
            case Some(nextPage) if nextPage.xs.nonEmpty =>
              val maybeNextPage = next(nextPage)
              stream(nextPage.xs, maybeNextPage)
            case _ =>
              Stream.empty[T]
          }
        case h +: t =>
          Stream.cons(h, stream(t, maybeNextPage))
      }
    }

    (getPage(path, params): Future[Page[T]]).map { p0: Page[T] => stream(p0.xs, next(p0)) }
  }

  def getPage[T: ClassTag](path: String, params: Map[String, String])
                          (implicit ec: ExecutionContext, de: CustomSerializer[T], m: Manifest[T]): Future[Page[T]] = {
    val absoluteUri = uri"$uri/$path?${params.updated("limit", "100")}"
    getPageAbsoluteUri(absoluteUri)
  }

  private def getPageAbsoluteUri[T: ClassTag](uri: Uri)(implicit ec: ExecutionContext, de: CustomSerializer[T],
                                                        m: Manifest[T]): Future[Page[T]] = {
    for {
      resp <- sttp.get(uri).response(asString).send()
    } yield {
      resp.body match {
        case Right(r) => Page(r, de)
        case Left(s) => throw TxnFailure(uri, s).getOrElse(new RuntimeException(s"Unrecognised response: $s"))
      }
    }
  }
}

object Server {
  def apply(uri: String): Try[Server] = Try {
    Server(URI.create(uri))
  }
}

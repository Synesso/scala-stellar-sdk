package stellar.sdk.inet

import java.net.URI

import akka.actor.ActorSystem
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp.json4s._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.Serialization
import org.json4s.{CustomSerializer, NoTypeHints}
import stellar.sdk.op.TransactedOperationDeserializer
import stellar.sdk.resp._
import stellar.sdk.{OrderBookDeserializer, SignedTransaction}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}


trait HorizonAccess {
  def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResp]

  def get[T: ClassTag](path: String, params: Map[String, Any] = Map.empty)
                      (implicit ec: ExecutionContext, m: Manifest[T]): Future[T]

  def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder, params: Map[String, String] = Map.empty)
                            (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]]

  def getPage[T: ClassTag](path: String, params: Map[String, String])
                          (implicit ec: ExecutionContext, de: CustomSerializer[T], m: Manifest[T]): Future[Page[T]]
}

class Horizon(uri: URI,
              system: ActorSystem = ActorSystem("stellar-sdk", ConfigFactory.load().getConfig("scala-stellar-sdk")))
  extends HorizonAccess with LazyLogging {

  implicit val backend = AkkaHttpBackend.usingActorSystem(system)
  implicit val formats = Serialization.formats(NoTypeHints) + AccountRespDeserializer + DataValueRespDeserializer +
    LedgerRespDeserializer + TransactedOperationDeserializer + OrderBookDeserializer + TransactionPostRespDeserializer

  def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResp] = {
    logger.debug(s"Posting {} {}", txn, txn.encodeXDR)
    val requestUri = uri"$uri/transactions"
    for {
      envelope <- Future(txn.encodeXDR)
      response <- sttp.body(Map("tx" -> envelope)).post(requestUri)
        .readTimeout(5 minutes)
        .response(asJson[TransactionPostResp]).send()
    } yield {
      response.body match {
        case Right(r) => r
        case Left(s) => throw TxnFailure(requestUri, s).getOrElse(new RuntimeException(s"Unrecognised response: $s"))
      }
    }
  }

  def get[T: ClassTag](path: String, params: Map[String, Any] = Map.empty)(implicit ec: ExecutionContext, m: Manifest[T]): Future[T] = {
    val uriPath = s"$uri$path"
    val requestUri = uri"$uriPath?$params"
    logger.debug(s"Getting {}", requestUri)

    sttp.get(requestUri).response(asJson[T]).send().map(_.body match {
      case Left(s) => throw TxnFailure(requestUri, s).getOrElse(new RuntimeException(s"Unrecognised response: $s"))
      case Right(r) => r
    })
  }

  def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder, params: Map[String, String] = Map.empty)
                            (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]] = {

    val cursorParam = cursor match {
      case Now => "now"
      case Record(r) => r.toString
    }
    val orderParam = order match {
      case Asc => "asc"
      case Desc => "desc"
    }
    val allParams = params ++ Map("cursor" -> cursorParam) ++ Map("order" -> orderParam)

    implicit val inner = de

    def next(p: Page[T]): Future[Option[Page[T]]] =
      (getPageAbsoluteUri(uri"${p.nextLink}".copy(port = Some(uri.getPort))): Future[Page[T]]).map(Some(_))
        .recover { case e: TxnFailure => None }

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

    (getPage(path, allParams): Future[Page[T]]).map { p0: Page[T] => stream(p0.xs, next(p0)) }
  }

  def getPage[T: ClassTag](path: String, params: Map[String, String])
                          (implicit ec: ExecutionContext, de: CustomSerializer[T], m: Manifest[T]): Future[Page[T]] = {
    val absoluteUri = uri"$uri/$path?${params.updated("limit", "100")}"
    getPageAbsoluteUri(absoluteUri)
  }

  private def getPageAbsoluteUri[T: ClassTag](uri: Uri)(implicit ec: ExecutionContext, de: CustomSerializer[T],
                                                        m: Manifest[T]): Future[Page[T]] = {
    logger.debug(s"Getting $uri")
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

object HorizonAccess {
  def apply(uri: String): Try[HorizonAccess] = Try(new Horizon(URI.create(uri)))
}

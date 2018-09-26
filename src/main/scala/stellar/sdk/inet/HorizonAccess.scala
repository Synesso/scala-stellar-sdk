package stellar.sdk.inet

import java.net.URI

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.native.{JsonMethods, Serialization}
import org.json4s.{CustomSerializer, DefaultFormats, Formats, NoTypeHints}
import stellar.sdk.op.TransactedOperationDeserializer
import stellar.sdk.resp._
import stellar.sdk.{HorizonCursor, HorizonOrder, OrderBookDeserializer, Record, SignedTransaction}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}


trait HorizonAccess {
  val `application/hal+json` = MediaType.applicationWithFixedCharset("hal+json", HttpCharsets.`UTF-8`)
  val `application/problem+json` = MediaType.applicationWithFixedCharset("problem+json", HttpCharsets.`UTF-8`)

  final object HalJsonSupport extends Json4sSupport {
    override def unmarshallerContentTypes = List(`application/json`, `application/hal+json`, `application/problem+json`)
  }

  def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResp]

  def get[T: ClassTag](path: String, params: Map[String, String] = Map.empty)
                      (implicit ec: ExecutionContext, m: Manifest[T]): Future[T]

  def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder, params: Map[String, String] = Map.empty)
                            (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]]

  def getSource[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, params: Map[String, String] = Map.empty)
    (implicit ec: ExecutionContext, m: Manifest[T]): Source[T, NotUsed]

}

class Horizon(uri: URI)
             (implicit system: ActorSystem = ActorSystem("stellar-sdk", ConfigFactory.load().getConfig("scala-stellar-sdk")))
  extends HorizonAccess with LazyLogging {

  import HalJsonSupport._

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val serialization = org.json4s.native.Serialization
  implicit val formats = Serialization.formats(NoTypeHints) + AccountRespDeserializer + DataValueRespDeserializer +
    LedgerRespDeserializer + TransactedOperationDeserializer + OrderBookDeserializer + TransactionPostRespDeserializer +
    TransactionHistoryRespDeserializer + TxnFailureDeserializer

  override def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResp] = {
    logger.debug(s"Posting {} {}", txn, txn.encodeXDR)
    for {
      envelope <- Future(txn.encodeXDR)
      request = HttpRequest(POST, Uri(s"$uri/transactions"), entity = FormData("tx" -> envelope).toEntity)
      response <- Http().singleRequest(request)
      unwrapped <- parseOrRedirectOrError[TransactionPostResp](request, response)
    } yield unwrapped.get
  }

  override def get[T: ClassTag](path: String, params: Map[String, String] = Map.empty)
                               (implicit ec: ExecutionContext, m: Manifest[T]): Future[T] = {
    val requestUri = Uri(s"$uri$path").withQuery(Query(params))
    logger.debug(s"Getting {}", requestUri)

    val request = HttpRequest(GET, requestUri)
    for {
      response <- Http().singleRequest(request)
      unwrapped <- parseOrRedirectOrError(request, response)
    } yield unwrapped.get
  }

  private def parseOrRedirectOrError[T](request: HttpRequest, response: HttpResponse)(implicit m: Manifest[T]): Future[Try[T]] = {
    val HttpResponse(status, _, entity, _) = response
    if (status.isRedirection()) {
      val request_ = request.copy(uri = response.header[Location].get.uri)
      Http().singleRequest(request).flatMap(parseOrRedirectOrError(request_, _))
    } else if (status.isSuccess()) Unmarshal(entity).to[T].map(Success(_))
    else Unmarshal(entity).to[TxnFailure].map(_.copy(uri = request.uri)).map(Failure(_))
  }

  override def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder,
                                      params: Map[String, String] = Map.empty)
                                     (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]] = {

    implicit val formats = DefaultFormats + RawPageDeserializer + de

    val query = Query(params ++ Map(
      "cursor" -> cursor.paramValue,
      "order" -> order.paramValue,
      "limit" -> "100"
    ))
    val requestUri = Uri(s"$uri$path").withQuery(query)
    
    def next(p: Page[T]): Future[Option[Page[T]]] =
      (getPage(Uri(p.nextLink).withPort(requestUri.effectivePort)): Future[Page[T]]).map(Some(_))
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

    (getPage(requestUri): Future[Page[T]]).map { p0: Page[T] => stream(p0.xs, next(p0)) }
  }

  def getPage[T: ClassTag](uri: Uri)
                          (implicit ec: ExecutionContext, m: Manifest[T], formats: Formats): Future[Page[T]] = {

    logger.debug(s"Getting $uri")
    for {
      response <- Http().singleRequest(HttpRequest(GET, uri))
      unwrapped <- Unmarshal(response).to[RawPage]
    } yield unwrapped.parse[T]
  }

  override def getSource[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, params: Map[String, String] = Map.empty)
                                     (implicit ec: ExecutionContext, m: Manifest[T]): Source[T, NotUsed] = {

    implicit val formats = DefaultFormats + de

    val query = Query(Map("cursor" -> cursor.paramValue) ++ params)
    val requestUri = Uri(s"$uri$path").withQuery(query)

    logger.debug(s"Streaming $requestUri")
    val lastEventId = cursor match {
      case Record(l) => Some(l.toString)
      case _ => None
    }

    val eventSource: Source[ServerSentEvent, NotUsed] =
      EventSource(requestUri, Http().singleRequest(_), lastEventId, unmarshaller = BigEventUnmarshalling)

    eventSource.mapConcat{ case ServerSentEvent(data, eventType, _, _) =>
      (if (eventType.contains("open")) None else Some(data)).to[collection.immutable.Iterable]
    }.map[T](JsonMethods.parse(_).extract[T])
  }
}

object HorizonAccess {
  def apply(uri: String): Try[HorizonAccess] = Try(new Horizon(URI.create(uri)))
}


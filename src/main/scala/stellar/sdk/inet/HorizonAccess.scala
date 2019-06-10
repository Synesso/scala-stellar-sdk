package stellar.sdk.inet

import java.net.URI
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.native.{JsonMethods, Serialization}
import org.json4s.{CustomSerializer, DefaultFormats, Formats, JObject, NoTypeHints}
import stellar.sdk.model._
import stellar.sdk.model.op.TransactedOperationDeserializer
import stellar.sdk.model.response._
import stellar.sdk.model.result.TransactionHistoryDeserializer
import stellar.sdk.{BuildInfo, DefaultActorSystem}

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

  def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResponse]

  def get[T: ClassTag](path: String, params: Map[String, String] = Map.empty)
                      (implicit ec: ExecutionContext, m: Manifest[T]): Future[T]

  def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder, params: Map[String, String] = Map.empty)
                            (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]]

  def getSource[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, params: Map[String, String] = Map.empty)
                            (implicit ec: ExecutionContext, m: Manifest[T]): Source[T, NotUsed]

}

class Horizon(call: HttpRequest => Future[HttpResponse])
             (implicit system: ActorSystem = DefaultActorSystem.system)
  extends HorizonAccess with LazyLogging {

  import HalJsonSupport._

  private val clientNameHeader = RawHeader("X-Client-Name", BuildInfo.name)
  private val clientVersionHeader = RawHeader("X-Client-Version", BuildInfo.version)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val serialization = org.json4s.native.Serialization
  implicit val formats = Serialization.formats(NoTypeHints) + AccountRespDeserializer + DataValueRespDeserializer +
    LedgerRespDeserializer + TransactedOperationDeserializer + OrderBookDeserializer +
    TransactionPostResponseDeserializer + TransactionHistoryDeserializer + FeeStatsRespDeserializer +
    NetworkInfoDeserializer

  override def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResponse] = {
    for {
      envelope <- Future(txn.encodeXDR)
      request = HttpRequest(POST, Uri(s"/transactions"), entity = FormData("tx" -> envelope).toEntity)
        .addHeader(clientNameHeader).addHeader(clientVersionHeader)
      response <- call(request)
      unwrapped <- parseOrRedirectOrError[TransactionPostResponse](request, response)
    } yield unwrapped.get
  }

  override def get[T: ClassTag](path: String, params: Map[String, String] = Map.empty)
                               (implicit ec: ExecutionContext, m: Manifest[T]): Future[T] = {
    val requestUri = Uri(s"$path").withQuery(Query(params))
    logger.debug(s"Getting {}", requestUri)

    val request = HttpRequest(GET, requestUri).addHeader(clientNameHeader).addHeader(clientVersionHeader)
    for {
      response <- call(request)
      unwrapped <- parseOrRedirectOrError(request, response)
    } yield unwrapped.get
  }

  private[inet] def parseOrRedirectOrError[T](request: HttpRequest, response: HttpResponse)
                                             (implicit m: Manifest[T]): Future[Try[T]] = {
    val HttpResponse(status, _, entity, _) = response

    // 404 - Not found
    if (status == NotFound)
      Unmarshal(response.entity).to[JObject]
        .map(HorizonEntityNotFound(request.uri, _)).map(Failure(_))

    // 429 - Rate limit exceeded
    else if (status == StatusCodes.TooManyRequests) {
      val retryAt = Try {
        Duration(
          response.headers.find(_.name() == "X-Ratelimit-Reset").map(_.value().toInt).getOrElse(5),
          TimeUnit.SECONDS
        )
      }
      Future.successful(HorizonRateLimitExceeded(request.uri, retryAt.get)).map(Failure(_))
    }

    // 3xx - Redirect
    else if (status.isRedirection()) {
      val request_ = request.copy(uri = response.header[Location].get.uri)
      Http().singleRequest(request_).flatMap(parseOrRedirectOrError(request_, _))
    }

    // 200 or 4xx
    else if (status.isSuccess() || status.intValue() < 500) Unmarshal(entity).to[T].map(Success(_))

    // 5xx
    else Unmarshal(response.entity).to[JObject].map(HorizonServerError(request.uri, _)).map(Failure(_))
  }

  override def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder,
                                      params: Map[String, String] = Map.empty)
                                     (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]] = {

    import scala.concurrent.duration._

    implicit val formats = DefaultFormats + RawPageDeserializer + de

    val query = Query(params ++ Map(
      "cursor" -> cursor.paramValue,
      "order" -> order.paramValue,
      "limit" -> "100"
    ))

    val requestUri = Uri(path).withQuery(query)

    def next(p: Page[T]): Future[Option[Page[T]]] =
      (getPage(Uri(p.nextLink)): Future[Page[T]]).map(Some(_))

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

    val request = HttpRequest(GET, uri).addHeader(clientNameHeader).addHeader(clientVersionHeader)

    val response =
      if (uri.isAbsolute) Http().singleRequest(request)
      else call(request)

    response.flatMap {
      case r if r.status == NotFound => Future(Page(Seq.empty[T], uri.toString()))
      case r                         => Unmarshal(r).to[RawPage].map(_.parse[T])
    }
    .recover { case t: Throwable => throw new RuntimeException(s"Unable to get page for $uri", t) }
  }

  override def getSource[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, params: Map[String, String] = Map.empty)
                                     (implicit ec: ExecutionContext, m: Manifest[T]): Source[T, NotUsed] = {

    implicit val formats = DefaultFormats + de
    implicit val sseUnmarshaller = BigEventUnmarshalling.fromEventsStream(system)

    val query = Query(Map("cursor" -> cursor.paramValue) ++ params)
    val requestUri = Uri(s"$path").withQuery(query)

    logger.debug(s"Streaming $requestUri")
    val lastEventId = cursor match {
      case Record(l) => Some(l.toString)
      case _ => None
    }

    def send(req: HttpRequest): Future[HttpResponse] =
      call(req.addHeader(clientNameHeader).addHeader(clientVersionHeader))

    val eventSource: Source[ServerSentEvent, NotUsed] =
      EventSource(requestUri, send, lastEventId)

    eventSource.mapConcat { case ServerSentEvent(data, eventType, _, _) =>
      (if (eventType.contains("open")) None else Some(data)).to[collection.immutable.Iterable]
    }.map[T](JsonMethods.parse(_).extract[T])
  }
}

object Horizon {

  def apply(uri: Uri)(implicit system: ActorSystem): HorizonAccess =
    Horizon.apply(
      request => {
        val requestUri = uri
          .withPath(request.uri.path)
          .withQuery(Uri.Query(request.uri.queryString()))
        Http().singleRequest(request.copy(uri = requestUri))
      }
    )

  def apply(uri: URI)(implicit system: ActorSystem = DefaultActorSystem.system): HorizonAccess =
    Horizon.apply(Uri(uri.toString))

  def apply(call: HttpRequest => Future[HttpResponse])(implicit system: ActorSystem): HorizonAccess =
    new Horizon(call)
}

/*
// TODO - Re-enable in separate PR with test coverage and documentation.
class MultiHostCaller(uris: NonEmptyList[Uri], maxNumberOfRetry: Int, backOff: FiniteDuration)
                     (implicit
                      system: ActorSystem) extends (HttpRequest => Future[HttpResponse]) {

  implicit val executionContext = system.dispatcher

  override def apply(request: HttpRequest): Future[HttpResponse] = {
    call(uris, request)(maxNumberOfRetry, backOff)
  }

  def call(uris: NonEmptyList[Uri], request: HttpRequest)
          (implicit
           maxNumberOfRetry: Int,
           minBackOff: FiniteDuration): Future[HttpResponse] = {

    singleRequest(prepareRequest(uris.head, request)) flatMap { response =>
      handleResponse(request, response) flatMap {
        case Success(result) => Future.successful(result)
        case Failure(_) => recover(uris.tail, request, response)
      }
    }
  }

  def recover(uris: List[Uri],
              request: HttpRequest, previousResponse: HttpResponse)
             (implicit
              maxNumberOfRetry: Int,
              minBackOff: FiniteDuration): Future[HttpResponse] = {
    uris match {
      case Nil =>
        Future.successful(previousResponse)

      case x :: xs =>
        singleRequest(prepareRequest(x, request)) flatMap { response =>
          handleResponse(request, response) flatMap {
            case Success(result) => Future.successful(result)
            case Failure(e) => recover(xs, request, response)
          }
        }
    }
  }

  def singleRequest(request: HttpRequest)
                   (implicit
                    maxNumberOfRetry: Int,
                    minBackOff: FiniteDuration): Future[HttpResponse] = {
    retryableFuture(() => Http().singleRequest(request))
  }

  def retryableFuture(future: () => Future[HttpResponse])
                     (implicit
                      maxNumberOfRetry: Int,
                      minBackOff: FiniteDuration): Future[HttpResponse] = {
    implicit val allNonFailed: retry.Success[HttpResponse] = retry.Success[HttpResponse] { _ => true }

    retry
      .BackOff(maxNumberOfRetry, minBackOff)
      .apply {
        future
      }
  }

  private def prepareRequest(base: Uri, request: HttpRequest) =
    request.copy(uri = getFullUri(base, request.uri))

  private def getFullUri(base: Uri, path: Uri): Uri =
    base.withPath(base.path ++ path.path)

  private def handleResponse(request: HttpRequest, response: HttpResponse): Future[Try[HttpResponse]] = {
    response.status match {
      case StatusCodes.NotFound =>
        Future.successful(Failure(FailedResponse(s"Request [$request] failed. Cause: [$response]")))

      case status if status.isRedirection() =>
        val request_ = request.copy(uri = response.header[Location].get.uri)
        Http().singleRequest(request).flatMap(handleResponse(request_, _))

      case status if status.isFailure() =>
        Future.successful(Failure(FailedResponse(s"Request [$request] failed. Cause: [$response]")))

      case _ =>
        Future.successful(Success(response))
    }
  }
}
*/


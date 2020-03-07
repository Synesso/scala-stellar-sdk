package stellar.sdk.inet

import java.net.HttpURLConnection.{HTTP_BAD_REQUEST, HTTP_NOT_FOUND}

import com.typesafe.scalalogging.LazyLogging
import okhttp3._
import okhttp3.logging.HttpLoggingInterceptor
import org.json4s.native.{JsonMethods, Serialization}
import org.json4s.{CustomSerializer, DefaultFormats, Formats, JObject, NoTypeHints}
import stellar.sdk.BuildInfo
import stellar.sdk.model.op.TransactedOperationDeserializer
import stellar.sdk.model.response.{TransactionPostResponse, _}
import stellar.sdk.model.result.TransactionHistoryDeserializer
import stellar.sdk.model.{HorizonCursor, HorizonOrder, SignedTransaction, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

class OkHorizon(base: HttpUrl) extends HorizonAccess with LazyLogging {

  implicit val serialization: Serialization.type = org.json4s.native.Serialization
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + AccountRespDeserializer +
    DataValueRespDeserializer + LedgerRespDeserializer + TransactedOperationDeserializer +
    OrderBookDeserializer + TransactionPostResponseDeserializer + TransactionHistoryDeserializer +
    FeeStatsRespDeserializer + NetworkInfoDeserializer

  private val client = new OkHttpClient.Builder()
    // .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
    .build()
  private val headers = Headers.of(
    "X-Client-Name", BuildInfo.name,
    "X-Client-Version", BuildInfo.version)

  override def post(txn: SignedTransaction)
                   (implicit ec: ExecutionContext): Future[TransactionPostResponse] = {

    val url = base.newBuilder().addPathSegment("transactions").build()
    Future(txn.encodeXDR)
      .map(envelope => new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("tx", envelope)
        .build)
      .map(body => new Request.Builder().url(url).post(body).headers(headers).build())
      .map(request => client.newCall(request).execute().body().string())
      .map(JsonMethods.parse(_).extract[TransactionPostResponse])
  }

  override def get[T: ClassTag](path: String, params: Map[String, String])
                               (implicit ec: ExecutionContext, m: Manifest[T]): Future[T] = {

    val url = params.foldLeft(base.newBuilder().addPathSegments(path)) {
      case (url, (k, v)) => url.addQueryParameter(k, v)
    }.build()
    logger.debug(s"Getting {}", url)
    val request = new Request.Builder().url(url).build()
    Future(client.newCall(request).execute())
      .map(_.body().string())
      .map(JsonMethods.parse(_))
      .map {
        case doc: JObject =>
          Try(doc.extract[T]) match {
            case Success(t) => t
            case Failure(_) => throw HorizonEntityNotFound(url, doc)
          }
        case js => throw HorizonEntityNotFound(url, js)
      }
  }

  override def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor,
                                      order: HorizonOrder, params: Map[String, String])
                                     (implicit ec: ExecutionContext, m: Manifest[T]): Future[LazyList[T]] = {

    // TODO (jem) - Cursor is temporarily removed because Horizon 1.0.0 cannot parse it.
//    Future(getStreamInternal(path, de, Some(cursor), Some(order), params))
    Future(getStreamInternal(path, de, None, Some(order), params))
  }

  override def getSeq[T: ClassTag](path: String, de: CustomSerializer[T], params: Map[String, String])
                                  (implicit ec: ExecutionContext, m: Manifest[T]): Future[LazyList[T]] = {
    Future(getStreamInternal(path, de, None, None, params))
  }

  private def getStreamInternal[T: ClassTag](path: String, de: CustomSerializer[T],
                                             cursor: Option[HorizonCursor], order: Option[HorizonOrder],
                                             params: Map[String, String])
                                            (implicit m: Manifest[T]): LazyList[T] = {
    implicit val formats: Formats = DefaultFormats + RawPageDeserializer + de

    val fullParams: Map[String, String] = params ++ Seq(
      cursor.map("cursor" -> _.paramValue),
      order.map("order" -> _.paramValue),
      Some("limit" -> "100")
    ).flatten.toMap
    val requestUrl = fullParams.foldLeft(base.newBuilder().addPathSegments(path)) {
      case (url, (k, v)) => url.addQueryParameter(k, v)
    }.build()

    def streamFromPage(buffer: List[T], nextLink: Option[HttpUrl]): LazyList[T] = {
      buffer match {
        case h :: t =>
          LazyList.cons(h, streamFromPage(t, nextLink))
        case _ =>
          nextLink
            .map { link =>
              val page = getPage[T](link)
              streamFromPage(page.xs, page.nextLink)
            }
            .getOrElse(LazyList.empty[T])
      }
    }

    streamFromPage(Nil, Some(requestUrl))
  }

  private def getPage[T: ClassTag](url: HttpUrl)
                                  (implicit m: Manifest[T], formats: Formats): Page[T] = {

    val response = client.newCall(new Request.Builder().url(url).build()).execute()
    PageParser.parse(url, response.code(), response.body().string())

  }
}

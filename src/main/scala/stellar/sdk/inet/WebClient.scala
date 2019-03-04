package stellar.sdk.inet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes.{NotFound, Redirection}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Location, RawHeader}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.Formats
import stellar.sdk.BuildInfo

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Failure

trait WebClient extends LazyLogging {

  val base: Uri

  implicit val system: ActorSystem
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val serialization = org.json4s.native.Serialization

  private val clientNameHeader = RawHeader("X-Client-Name", BuildInfo.name)
  private val clientVersionHeader = RawHeader("X-Client-Version", BuildInfo.version)

  val `application/hal+json` = MediaType.applicationWithFixedCharset("hal+json", HttpCharsets.`UTF-8`)
  val `application/problem+json` = MediaType.applicationWithFixedCharset("problem+json", HttpCharsets.`UTF-8`)

  final object HalJsonSupport extends Json4sSupport {
    override def unmarshallerContentTypes = List(`application/json`, `application/hal+json`, `application/problem+json`)
  }

  import HalJsonSupport._

  def get[T: ClassTag](path: Path, params: Map[String, String] = Map.empty)
    (implicit ec: ExecutionContext, formats: Formats, m: Manifest[T]): Future[Option[T]] = {

    val requestUri = base.withPath(path).withQuery(Query(params))

    val request = HttpRequest(GET, requestUri).addHeader(clientNameHeader).addHeader(clientVersionHeader)
    for {
      response <- singleRequest(request)
      unwrapped <- parse[T](request, response)(unmarshaller[T])
    } yield unwrapped
  }

  private def parse[T](request: HttpRequest, response: HttpResponse)
                      (implicit um: Unmarshaller[ResponseEntity, T]): Future[Option[T]] = {

    val HttpResponse(status, _, entity, _) = response

    status match {
      case NotFound => Future(None)
      case _ if status.isRedirection =>
        val request_ = request.copy(uri = response.header[Location].get.uri.withQuery(request.uri.query()))
        singleRequest(request_).flatMap(parse(request_, _))
      case _ if status.isFailure =>
        Unmarshal(entity).to[String].map(e => throw RestException(s"${status.reason} - $e"))
      case _ => Unmarshal(entity).to[T].map(Some(_)).recoverWith { case t: Throwable =>
        throw RestException(s"Could not parse entity to target type: ${t.getMessage}")
      }
    }
  }

  private def singleRequest(request: HttpRequest): Future[HttpResponse] = {
    logger.debug(s"Getting {}", request.uri)
    Http().singleRequest(request)
  }
}

/**
  * Indicates that something went wrong with a REST operation.
  */
case class RestException(message: String) extends Exception(message)
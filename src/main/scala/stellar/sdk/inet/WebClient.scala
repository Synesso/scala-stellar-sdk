package stellar.sdk.inet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.Formats
import stellar.sdk.BuildInfo

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

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
    (implicit ec: ExecutionContext, formats: Formats, m: Manifest[T]): Future[T] = {

    val requestUri = base.withPath(path).withQuery(Query(params))
    logger.debug(s"Getting {}", requestUri)

    val request = HttpRequest(GET, requestUri).addHeader(clientNameHeader).addHeader(clientVersionHeader)
    for {
      response <- Http().singleRequest(request)
      unwrapped <- parse[T](request, response)(unmarshaller[T])
    } yield unwrapped
  }

  private def parse[T](request: HttpRequest, response: HttpResponse)
                      (implicit um: Unmarshaller[ResponseEntity, T]): Future[T] = {

    val HttpResponse(status, _, entity, _) = response

    Unmarshal(entity).to[T]

  }
}

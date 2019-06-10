package stellar.sdk.inet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Location, RawHeader}
import org.json4s.{DefaultFormats, DefaultJsonFormats}
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._

class HorizonSpec(implicit ec: ExecutionEnv) extends Specification with Mockito {

  implicit val system = ActorSystem()

  val uri = Uri("https://test/")
  val request = new HttpRequest(HttpMethods.GET, uri, Nil, HttpEntity(""), HttpProtocols.`HTTP/2.0`)

  def call(uri: Uri): HttpRequest => Future[HttpResponse] =
    request => Http().singleRequest(request.copy(uri = request.uri.withPath(uri.path ++ request.uri.path)))

  "parsing a not-found response" should {
    "fail with a not found entity" >> {
      val responseBody = ("detail" -> "thing not found") ~ ("status" -> 404)
      val responseEntity = HttpEntity(
        ContentType(MediaType.applicationWithFixedCharset("problem+json", `UTF-8`)),
        compact(render(responseBody))
      )
      val response = new HttpResponse(StatusCodes.NotFound, Nil, responseEntity, HttpProtocols.`HTTP/2.0`)
      new Horizon(call(uri)).parseOrRedirectOrError[String](request, response) must beFailedTry[String].like {
        case HorizonEntityNotFound(requestUri, doc) =>
          requestUri mustEqual uri
          doc mustEqual responseBody
      }.awaitFor(1.second)
    }
  }

  "parsing a rate-limit-exceeded response" should {
    "fail with rate-limit-exceeded" >> {
      val responseBody = ("detail" -> "going too fast!") ~ ("status" -> 429)
      val responseEntity = HttpEntity(
        ContentType(MediaType.applicationWithFixedCharset("problem+json", `UTF-8`)),
        compact(render(responseBody))
      )
      val headers = immutable.Seq(RawHeader("X-Ratelimit-Reset", "7"))
      val response = new HttpResponse(StatusCodes.TooManyRequests, headers, responseEntity, HttpProtocols.`HTTP/2.0`)
      new Horizon(call(uri)).parseOrRedirectOrError[String](request, response) must beFailedTry[String].like {
        case HorizonRateLimitExceeded(requestUri, retryAfter) =>
          requestUri mustEqual uri
          retryAfter mustEqual 7.seconds
      }.awaitFor(1.second)
    }

    "default to a retry value of 5 seconds" >> {
      val responseBody = ("detail" -> "going too fast!") ~ ("status" -> 429)
      val responseEntity = HttpEntity(
        ContentType(MediaType.applicationWithFixedCharset("problem+json", `UTF-8`)),
        compact(render(responseBody))
      )
      val response = new HttpResponse(StatusCodes.TooManyRequests, Nil, responseEntity, HttpProtocols.`HTTP/2.0`)
      new Horizon(call(uri)).parseOrRedirectOrError[String](request, response) must beFailedTry[String].like {
        case HorizonRateLimitExceeded(requestUri, retryAfter) =>
          requestUri mustEqual uri
          retryAfter mustEqual 5.seconds
      }.awaitFor(1.second)
    }
  }

  "parsing a server error" should {
    "fail with a horizon server error" >> {
      val responseBody = ("detail" -> "something's broken") ~ ("status" -> 500)
      val responseEntity = HttpEntity(
        ContentType(MediaType.applicationWithFixedCharset("problem+json", `UTF-8`)),
        compact(render(responseBody))
      )
      val response = new HttpResponse(StatusCodes.InternalServerError, Nil, responseEntity, HttpProtocols.`HTTP/2.0`)
      new Horizon(call(uri)).parseOrRedirectOrError[String](request, response) must beFailedTry[String].like {
        case HorizonServerError(requestUri, doc) =>
          requestUri mustEqual uri
          doc mustEqual responseBody
      }.awaitFor(1.second)
    }
  }
}

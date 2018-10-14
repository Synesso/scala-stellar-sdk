package stellar.sdk.inet

import java.net.URI

import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import org.json4s.native.JsonMethods._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.json4s.JsonDSL._
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.duration._
import scala.concurrent.Await

class HorizonSpec(implicit ec: ExecutionEnv) extends Specification with Mockito {

  "creating a server from string" should {
    "fail when uri is invalid" >> {
      HorizonAccess("fruit:\\foo") must beFailedTry[HorizonAccess]
    }
    "succeed when uri is compliant" >> {
      HorizonAccess("https://horizon.stellar.org") must beSuccessfulTry[HorizonAccess]
    }
  }

  val uri = Uri("https://test/")
  val request = new HttpRequest(HttpMethods.GET, uri, Nil, HttpEntity(""), HttpProtocols.`HTTP/2.0`)

  "parsing a not-found response" should {
    "fail with a not found entity" >> {
      val responseBody = ("detail" -> "thing not found") ~ ("status" -> 404)
      val responseEntity = HttpEntity(
        ContentType(MediaType.applicationWithFixedCharset("problem+json", `UTF-8`)),
        compact(render(responseBody))
      )
      val response = new HttpResponse(StatusCodes.NotFound, Nil, responseEntity, HttpProtocols.`HTTP/2.0`)
      new Horizon(URI.create("https://test/")).parseOrRedirectOrError[String](request, response) must beFailedTry[String].like {
        case HorizonEntityNotFound(requestUri, doc) =>
          requestUri mustEqual uri
          doc mustEqual responseBody
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
      new Horizon(URI.create("https://test/")).parseOrRedirectOrError[String](request, response) must beFailedTry[String].like {
        case HorizonServerError(requestUri, doc) =>
          requestUri mustEqual uri
          doc mustEqual responseBody
      }.awaitFor(1.second)
    }
  }

}

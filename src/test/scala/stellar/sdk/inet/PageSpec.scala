package stellar.sdk.inet

import java.net.HttpURLConnection.{HTTP_BAD_REQUEST, HTTP_NOT_FOUND}

import okhttp3.HttpUrl
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods
import org.specs2.mutable.Specification
import stellar.sdk.model.response.ResponseParser

class PageSpec extends Specification {

  implicit val formats = DefaultFormats + RawPageDeserializer + HelloDeserializer

  "page parsing" should {
    "return an empty page if no results were found" >> {
      val page = PageParser.parse[String](HttpUrl.parse("http://localhost/"), HTTP_NOT_FOUND, "")
      page.xs must beEmpty
    }

    "throw a bad request exception with the reasons when provided" >> {
      val url = HttpUrl.parse("http://localhost/")
      PageParser.parse[String](url, HTTP_BAD_REQUEST,
        """{
          |  "type": "https://stellar.org/horizon-errors/bad_request",
          |  "title": "Bad Request",
          |  "status": 400,
          |  "detail": "The request you sent was invalid in some way.",
          |  "extras": {
          |    "invalid_field": "cursor",
          |    "reason": "cursor must contain exactly one colon"
          |  }
          |}""".stripMargin) must throwA[HorizonBadRequest].like { e =>
        e.getMessage mustEqual "Bad request. http://localhost/ -> cursor must contain exactly one colon"
      }
    }

    "throw a bad request exception with the full document when the reason is not provided" >> {
      val url = HttpUrl.parse("http://localhost/")
      PageParser.parse[String](url, HTTP_BAD_REQUEST, "random text") must throwA[HorizonBadRequest].like { e =>
        e.getMessage mustEqual "Bad request. http://localhost/ -> random text"
      }
    }

    "parse the member values and provide a link to the next page" >> {
      val doc =
        """
          |{
          |  "_links": {
          |    "self": {
          |      "href": "https://horizon-testnet.stellar.org/hello?cursor=\u0026limit=10\u0026order=asc"
          |    },
          |    "next": {
          |      "href": "https://horizon-testnet.stellar.org/hello?cursor=2045052972961793-0\u0026limit=10\u0026order=asc"
          |    },
          |    "prev": {
          |      "href": "https://horizon-testnet.stellar.org/hello?cursor=940258535411713-0\u0026limit=10\u0026order=desc"
          |    }
          |  },
          |  "_embedded": {
          |    "records": [
          |      {"hello":"world"},
          |      {"hello":"whirled"}
          |    ]
          |  }
          |}
        """.stripMargin

      JsonMethods.parse(doc).extract[RawPage].parse[String](HttpUrl.parse("http://localhost/")) mustEqual Page(
        List("world", "whirled"),
        nextLink = Some(HttpUrl.parse("https://horizon-testnet.stellar.org/hello?cursor=2045052972961793-0&limit=10&order=asc"))
      )
    }
  } 

  object HelloDeserializer extends ResponseParser[String]({ o: JObject =>
    implicit val formats = DefaultFormats
    (o \ "hello").extract[String]
  })

}

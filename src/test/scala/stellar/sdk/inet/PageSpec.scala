package stellar.sdk.inet

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods
import org.specs2.mutable.Specification
import stellar.sdk.model.response.ResponseParser

class PageSpec extends Specification {

  "constructing a page from json" should {
    "provide a link to the next page" >> {
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
          |      {"hello":"world"}
          |    ]
          |  }
          |}
        """.stripMargin

      implicit val formats = DefaultFormats + RawPageDeserializer + HelloDeserializer
      JsonMethods.parse(doc).extract[RawPage].parse[String] mustEqual Page(Seq("world"),
        nextLink = "https://horizon-testnet.stellar.org/hello?cursor=2045052972961793-0&limit=10&order=asc"
      )
    }
  }

  object HelloDeserializer extends ResponseParser[String]({ o: JObject =>
    implicit val formats = DefaultFormats
    (o \ "hello").extract[String]
  })

}

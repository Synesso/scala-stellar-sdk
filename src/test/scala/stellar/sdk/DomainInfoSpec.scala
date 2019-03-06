package stellar.sdk

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import stellar.sdk.StubServer.ReplyWithText

import scala.concurrent.duration._

class DomainInfoSpec(implicit ee: ExecutionEnv) extends Specification with AfterAll {

  val server = new StubServer()

  override def afterAll(): Unit = server.stop()

  "domain info lookup" should {
    "successfully find federation server in TOML" >> {
      server.expectGet(".well-known/stellar.toml", Map.empty, ReplyWithText(
        """FEDERATION_SERVER="https://xyz.com/fedsrv""""
      ))

      DomainInfo.forDomain("http://localhost:8002") must beSome(
        DomainInfo(FederationServer(Uri("https://xyz.com"), Path("/fedsrv")))
      ).awaitFor(5.seconds)
    }
  }

  "parsing toml" should {
    "be successful when endpoint is correctly defined" >> {
      DomainInfo.from("""FEDERATION_SERVER="https://fruitcakes.com/federation.pl"""") mustEqual
        DomainInfo(FederationServer("https://fruitcakes.com/federation.pl"))
    }

    "be successful when there is other content in the file" >> {
      DomainInfo.from(
        """
          |# this is how we roll
          |ROLL="like this"
          |NUMBERS = [1, 2, 3]
          |FEDERATION_SERVER="bonanza"
          |[section_9]
          |NUMBERS = [[66, 22],[33,99]]
          |FEDERATION_SERVER="ignoreme"
        """.stripMargin) mustEqual
        DomainInfo(FederationServer("bonanza"))
    }

    "fail when the document is empty" >> {
      DomainInfo.from("") must throwA[DomainInfoParseException]
    }

    "fail when the document is something other than TOML" >> {
      DomainInfo.from("""{'pandas': 12}""") must throwA[DomainInfoParseException]
    }

    "fail when the endpoint is defined as something other than a string" >> {
      DomainInfo.from("""FEDERATION_SERVER=17""") must throwA[DomainInfoParseException]
    }

    "fail when the endpoint is not defined" >> {
      DomainInfo.from("""THANKS_FOR_THE_FISH=42""") must throwA[DomainInfoParseException]
    }
  }

}

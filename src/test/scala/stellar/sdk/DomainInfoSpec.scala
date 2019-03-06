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

}
